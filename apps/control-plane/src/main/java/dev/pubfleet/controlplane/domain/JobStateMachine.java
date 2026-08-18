package dev.pubfleet.controlplane.domain;

import dev.pubfleet.contracts.JobStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The only guard on the job status machine.
 *
 * <p>PENDING to CLAIMED to SUCCEEDED or FAILED. Nothing else is legal, and that
 * includes a status that stays the same. Every write of the status column goes through
 * {@link #assertLegal}. The control plane holds this rule because the control plane
 * owns the job record. The worker owns the claim only.
 */
public final class JobStateMachine {

    private static final Map<JobStatus, Set<JobStatus>> LEGAL_NEXT =
            new EnumMap<>(JobStatus.class);

    static {
        LEGAL_NEXT.put(JobStatus.PENDING, EnumSet.of(JobStatus.CLAIMED));
        LEGAL_NEXT.put(JobStatus.CLAIMED, EnumSet.of(JobStatus.SUCCEEDED, JobStatus.FAILED));
        LEGAL_NEXT.put(JobStatus.SUCCEEDED, EnumSet.noneOf(JobStatus.class));
        LEGAL_NEXT.put(JobStatus.FAILED, EnumSet.noneOf(JobStatus.class));
    }

    private JobStateMachine() {
    }

    /** The statuses a job in {@code from} can move to. Empty for a terminal status. */
    public static Set<JobStatus> legalNext(JobStatus from) {
        return Set.copyOf(LEGAL_NEXT.get(from));
    }

    /** True when a job in {@code from} can move to {@code to}. */
    public static boolean isLegal(JobStatus from, JobStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return LEGAL_NEXT.get(from).contains(to);
    }

    /**
     * Lets a legal transition through and stops an illegal one.
     *
     * @throws IllegalJobTransitionException if the move is not legal
     */
    public static void assertLegal(UUID jobId, JobStatus from, JobStatus to) {
        if (!isLegal(from, to)) {
            throw new IllegalJobTransitionException(jobId, from, to);
        }
    }
}
