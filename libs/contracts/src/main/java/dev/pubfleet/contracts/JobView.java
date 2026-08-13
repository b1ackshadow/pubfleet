package dev.pubfleet.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A job as the API shows it. The JPA entity never crosses the HTTP boundary.
 *
 * @param workerId null until a worker claims the job
 * @param result   null until the job reaches a terminal state
 */
public record JobView(
        UUID id,
        String supplierId,
        String payloadRef,
        JobStatus status,
        String workerId,
        String result,
        Instant createdAt,
        Instant updatedAt) {
}
