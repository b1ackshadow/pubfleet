package dev.pubfleet.controlplane.messaging;

import dev.pubfleet.contracts.JobCompletedEvent;
import dev.pubfleet.controlplane.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reads the worker's outcome and asks the control plane to write it.
 *
 * <p>Unit 00 has no retry policy, no dead letter topic, and no idempotency key. A
 * repeated event hits the state machine guard and fails, because a terminal status
 * cannot move again. Units 01 to 03 own that. See
 * {@code units/00-walking-skeleton/CONTEXT.md}.
 */
@Component
public class JobCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobCompletedConsumer.class);

    private final JobService jobs;

    public JobCompletedConsumer(JobService jobs) {
        this.jobs = jobs;
    }

    @KafkaListener(topics = JobCompletedEvent.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void onJobCompleted(JobCompletedEvent event) {
        log.debug("Received completion for job {} with status {}",
                event.jobId(), event.status());

        jobs.applyCompletion(event);
    }
}
