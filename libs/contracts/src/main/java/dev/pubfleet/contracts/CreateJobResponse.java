package dev.pubfleet.contracts;

import java.util.UUID;

/**
 * The 202 body of {@code POST /api/jobs}.
 *
 * @param jobId  the new job id
 * @param status always {@link JobStatus#PENDING} at this point
 */
public record CreateJobResponse(UUID jobId, JobStatus status) {
}
