package dev.pubfleet.controlplane.domain;

import java.util.UUID;

/** Thrown when a job id has no row. The web layer turns this into a 404. */
public class JobNotFoundException extends RuntimeException {

    private final transient UUID jobId;

    public JobNotFoundException(UUID jobId) {
        super("Job " + jobId + " was not found");
        this.jobId = jobId;
    }

    public UUID jobId() {
        return jobId;
    }
}
