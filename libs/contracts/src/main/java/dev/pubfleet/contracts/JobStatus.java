package dev.pubfleet.contracts;

/**
 * The life of a job.
 *
 * <p>PENDING to CLAIMED to SUCCEEDED or FAILED. No other transition is legal.
 * The control plane holds the only guard that enforces this. See
 * {@code dev.pubfleet.controlplane.domain.JobStateMachine}.
 */
public enum JobStatus {

    /** The control plane wrote the row. No worker holds it. */
    PENDING,

    /** A worker holds the row and is doing the work. */
    CLAIMED,

    /** Terminal. The work finished and gave a result. */
    SUCCEEDED,

    /** Terminal. The work stopped with an error. */
    FAILED;

    /** True when this status is an end state. */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
