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
                try {
                    process(id);
                } catch (RuntimeException unreported) {
                    // The report itself failed. The job stays CLAIMED and no wrong
                    // outcome goes out. One broken report must not skip the rest of
                    // the batch.
                    log.error("Could not report the outcome of job {}", id, unreported);
                }
            } else {
                // Another worker won the row, or it is no longer PENDING. Both are fine.
                log.debug("Lost the claim on job {}", id);
            }
        }
    }

    /**
     * Runs one claimed job and reports it once.
     *
     * <p>The try block computes the outcome and nothing else. {@link #report} sits
     * after it on purpose: inside, a publish that threw would be caught as job failure,
     * so work that had already succeeded would be reported FAILED with a broker error
     * as its result.
     */
    private void process(UUID id) {
        log.info("Claimed job {}", id);

        JobStatus status;
        String result;
        try {
            result = runner.run(claims.findPayloadRef(id));
            status = JobStatus.SUCCEEDED;
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while working on job {}", id);
            status = JobStatus.FAILED;
            result = "worker was interrupted";
        } catch (RuntimeException failure) {
            log.warn("Job {} failed", id, failure);
            status = JobStatus.FAILED;
            result = describe(failure);
        }

        report(id, status, result);
    }

    /**
     * A result string for a failure. An exception with no message must not store the
     * literal text {@code "null"} in the job row, so the class name is used instead.
     */
    private static String describe(RuntimeException failure) {
        String message = failure.getMessage();

        return (message == null || message.isBlank())
                ? failure.getClass().getName() + " with no message"
                : message;
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
