package dev.pubfleet.controlplane.domain;

import dev.pubfleet.contracts.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static dev.pubfleet.contracts.JobStatus.CLAIMED;
import static dev.pubfleet.contracts.JobStatus.FAILED;
import static dev.pubfleet.contracts.JobStatus.PENDING;
import static dev.pubfleet.contracts.JobStatus.SUCCEEDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The guard is the whole status machine. If it lets a bad edge through, a worker can
 * push a job straight from PENDING to SUCCEEDED and the claim stops meaning anything.
 *
 * <p>The tests below name every legal edge, then sweep the full 4 by 4 grid and demand
 * that everything not named is refused.
 */
class JobStateMachineTest {

    private static final UUID JOB_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @ParameterizedTest(name = "{0} to {1} is allowed")
    @CsvSource({
            "PENDING, CLAIMED",
            "CLAIMED, SUCCEEDED",
            "CLAIMED, FAILED"
    })
    void allowsTheThreeLegalEdges(JobStatus from, JobStatus to) {
        assertThat(JobStateMachine.isLegal(from, to)).isTrue();
        assertThatCode(() -> JobStateMachine.assertLegal(JOB_ID, from, to))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesTheJumpThatSkipsTheClaim() {
        assertThatThrownBy(() -> JobStateMachine.assertLegal(JOB_ID, PENDING, SUCCEEDED))
                .isInstanceOf(IllegalJobTransitionException.class)
                .hasMessageContaining(JOB_ID.toString())
                .hasMessageContaining("PENDING")
                .hasMessageContaining("SUCCEEDED");
    }

    @Test
    void refusesTheJumpFromPendingToFailed() {
        assertThat(JobStateMachine.isLegal(PENDING, FAILED)).isFalse();
    }

    @Test
    void refusesGoingBackwards() {
        assertThat(JobStateMachine.isLegal(CLAIMED, PENDING)).isFalse();
        assertThat(JobStateMachine.isLegal(SUCCEEDED, CLAIMED)).isFalse();
        assertThat(JobStateMachine.isLegal(FAILED, PENDING)).isFalse();
    }

    @ParameterizedTest(name = "{0} is a dead end")
    @EnumSource(value = JobStatus.class, names = {"SUCCEEDED", "FAILED"})
    void refusesEveryMoveOutOfATerminalStatus(JobStatus terminal) {
        assertThat(JobStateMachine.legalNext(terminal)).isEmpty();

        for (JobStatus to : JobStatus.values()) {
            assertThatThrownBy(() -> JobStateMachine.assertLegal(JOB_ID, terminal, to))
                    .isInstanceOf(IllegalJobTransitionException.class);
        }
    }

    @ParameterizedTest(name = "{0} cannot move to itself")
    @EnumSource(JobStatus.class)
    void refusesAMoveToTheSameStatus(JobStatus status) {
        // A repeated completion event is the real case. Idempotency is unit 03's job,
        // so here the second one must be refused rather than quietly accepted.
        assertThat(JobStateMachine.isLegal(status, status)).isFalse();
    }

    @Test
    void refusesNulls() {
        assertThat(JobStateMachine.isLegal(null, CLAIMED)).isFalse();
        assertThat(JobStateMachine.isLegal(PENDING, null)).isFalse();
        assertThat(JobStateMachine.isLegal(null, null)).isFalse();
    }

    @Test
    void allowsNothingOutsideTheThreeNamedEdges() {
        int allowed = 0;

        for (JobStatus from : JobStatus.values()) {
            for (JobStatus to : JobStatus.values()) {
                if (JobStateMachine.isLegal(from, to)) {
                    allowed++;
                }
            }
        }

        assertThat(allowed).isEqualTo(3);
    }

    @Test
    void carriesTheRefusedEdgeOnTheException() {
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalJobTransitionException.class,
                () -> JobStateMachine.assertLegal(JOB_ID, SUCCEEDED, FAILED));

        assertThat(thrown.jobId()).isEqualTo(JOB_ID);
        assertThat(thrown.from()).isEqualTo(SUCCEEDED);
        assertThat(thrown.to()).isEqualTo(FAILED);
    }
}
