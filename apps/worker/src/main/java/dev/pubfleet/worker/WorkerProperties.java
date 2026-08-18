package dev.pubfleet.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Worker settings under the {@code pubfleet.worker} prefix.
 *
 * <p>{@code pubfleet.worker.poll-delay} is not a component here. Spring reads it as a
 * placeholder on the {@code @Scheduled} method of {@link JobPoller}, because the
 * scheduler needs it before any bean is built.
 *
 * @param id        the name this worker writes into {@code jobs.worker_id}
 * @param batchSize how many PENDING ids one poll looks at
 * @param workDelay how long the trivial work pretends to take
 */
@ConfigurationProperties(prefix = "pubfleet.worker")
public record WorkerProperties(String id, int batchSize, Duration workDelay) {

    public WorkerProperties {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("pubfleet.worker.id must be set");
        }
        if (batchSize < 1) {
            batchSize = 1;
        }
        if (workDelay == null) {
            workDelay = Duration.ofMillis(200);
        }
    }
}
