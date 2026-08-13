package dev.pubfleet.controlplane.domain;

import dev.pubfleet.contracts.JobStatus;

import java.util.UUID;

/** Thrown when something tries to move a job along an edge that does not exist. */
public class IllegalJobTransitionException extends RuntimeException {

    private final transient UUID jobId;
    private final transient JobStatus from;
    private final transient JobStatus to;

    public IllegalJobTransitionException(UUID jobId, JobStatus from, JobStatus to) {
        super("Job " + jobId + " cannot move from " + from + " to " + to);
        this.jobId = jobId;
        this.from = from;
        this.to = to;
    }

    public UUID jobId() {
        return jobId;
    }

    public JobStatus from() {
        return from;
    }

    public JobStatus to() {
        return to;
    }
}
