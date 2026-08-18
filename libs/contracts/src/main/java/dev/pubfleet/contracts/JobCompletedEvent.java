package dev.pubfleet.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * What the worker publishes to {@link #TOPIC} when the work stops.
 *
 * <p>The record is keyed by the job id on the wire. The status is always terminal:
 * the worker reports an outcome, and the control plane writes it. The worker never
 * writes a terminal state itself.
 *
 * @param status  {@link JobStatus#SUCCEEDED} or {@link JobStatus#FAILED}
 * @param result  the work output, or the error text on a failure
 */
public record JobCompletedEvent(
        UUID jobId,
        String workerId,
        JobStatus status,
        String result,
        Instant completedAt) {

    /** The only Kafka topic in unit 00. */
    public static final String TOPIC = "pubfleet.job.completed";
}
