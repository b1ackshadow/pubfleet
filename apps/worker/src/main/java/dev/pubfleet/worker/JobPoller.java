package dev.pubfleet.worker;

import dev.pubfleet.contracts.JobCompletedEvent;
import dev.pubfleet.contracts.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Polls Postgres on a fixed delay, claims what it can, and reports each outcome.
 *
 * <p>Postgres holds the work queue and Kafka carries the result. A broker that
 * dispatched the work instead would tie parallelism to the partition count and would
 * give no per-message acknowledgement.
 */
@Component
public class JobPoller {

    private static final Logger log = LoggerFactory.getLogger(JobPoller.class);

    private final JobClaimRepository claims;
    private final JobRunner runner;
    private final JobCompletedPublisher publisher;
    private final WorkerProperties properties;
    private final Clock clock;

    public JobPoller(JobClaimRepository claims, JobRunner runner,
                     JobCompletedPublisher publisher, WorkerProperties properties,
                     Clock clock) {
        this.claims = claims;
        this.runner = runner;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * One poll.
     *
     * <p>A fixed delay, not a fixed rate: the next poll starts after this one ends, so
     * a slow poll cannot pile work onto itself.
     */
    @Scheduled(fixedDelayString = "${pubfleet.worker.poll-delay:500ms}")
    public void poll() {
        List<UUID> candidates = claims.findPendingIds(properties.batchSize());

        for (UUID id : candidates) {
            if (claims.claim(id, properties.id())) {
                process(id);
            } else {
                // Another worker won the row, or it is no longer PENDING. Both are fine.
                log.debug("Lost the claim on job {}", id);
            }
        }
    }

    private void process(UUID id) {
        log.info("Claimed job {}", id);
        try {
            String result = runner.run(claims.findPayloadRef(id));
            report(id, JobStatus.SUCCEEDED, result);
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while working on job {}", id);
            report(id, JobStatus.FAILED, "worker was interrupted");
        } catch (RuntimeException failure) {
            log.warn("Job {} failed", id, failure);
            report(id, JobStatus.FAILED, String.valueOf(failure.getMessage()));
        }
    }

    /**
     * Reports the outcome. The worker asks. The control plane decides and writes the
     * terminal status. The worker never writes it itself.
     */
    private void report(UUID id, JobStatus status, String result) {
        publisher.publish(new JobCompletedEvent(
                id, properties.id(), status, result, clock.instant()));
    }
}
