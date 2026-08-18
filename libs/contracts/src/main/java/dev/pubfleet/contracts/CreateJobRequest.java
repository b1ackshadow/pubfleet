package dev.pubfleet.contracts;

/**
 * The body of {@code POST /api/jobs}.
 *
 * @param supplierId the supplier that sent the job
 * @param payloadRef a pointer to the work. Unit 00 does not read it as a pointer.
 */
public record CreateJobRequest(String supplierId, String payloadRef) {
}
