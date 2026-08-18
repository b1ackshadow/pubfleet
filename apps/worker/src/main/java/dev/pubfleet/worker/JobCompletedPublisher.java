package dev.pubfleet.worker;

import dev.pubfleet.contracts.JobCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes the outcome to {@link JobCompletedEvent#TOPIC}, keyed by the job id.
 *
 * <p>The key puts every event for one job on one partition, so the control plane sees
 * them in order.
 *
 * <p>The publish is not atomic with the claim. A crash between the two loses the event
 * and strands the job. That is unit 03's outbox. Do not fix it here. See
 * {@code units/00-walking-skeleton/CONTEXT.md}.
 */
@Component
public class JobCompletedPublisher {

    private final KafkaTemplate<String, JobCompletedEvent> kafka;

    public JobCompletedPublisher(KafkaTemplate<String, JobCompletedEvent> kafka) {
        this.kafka = kafka;
    }

    public void publish(JobCompletedEvent event) {
        kafka.send(JobCompletedEvent.TOPIC, event.jobId().toString(), event);
    }
}
