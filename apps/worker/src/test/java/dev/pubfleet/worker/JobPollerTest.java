package dev.pubfleet.worker;

import dev.pubfleet.contracts.JobCompletedEvent;
import dev.pubfleet.contracts.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The poll loop, driven directly. No broker and no database: the claim and the publish
 * are the two edges of this class, and both are doubles here.
 *
 * <p>{@link JobLifecycleIT} in the control plane proves the wire. What it cannot reach is
 * the branch taken when the work throws, and what the outcome is when the publish itself
 * throws. Those are the two tests that matter below.
 */
@ExtendWith(MockitoExtension.class)
class JobPollerTest {

    private static final String WORKER_ID = "worker-test";
    private static final Instant NOW = Instant.parse("2026-08-13T09:00:00Z");
    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

    private final WorkerProperties properties =
            new WorkerProperties(WORKER_ID, 5, Duration.ZERO);

    @Mock
    private JobClaimRepository claims;

    @Mock
    private JobCompletedPublisher publisher;

    @Captor
    private ArgumentCaptor<JobCompletedEvent> published;

    private JobPoller pollerWith(JobRunner runner) {
        return new JobPoller(claims, runner, publisher, properties, FIXED);
    }

    private JobPoller poller() {
        return pollerWith(new JobRunner(Duration.ZERO));
    }

    @Test
    @DisplayName("a claimed job runs and is reported SUCCEEDED with the runner's result")
    void aClaimedJobIsReportedSucceeded() {
        UUID id = UUID.randomUUID();
        given(claims.findPendingIds(5)).willReturn(List.of(id));
        given(claims.claim(id, WORKER_ID)).willReturn(true);
        given(claims.findPayloadRef(id)).willReturn("s3://bucket/one");

        poller().poll();

        verify(publisher, times(1)).publish(published.capture());
        JobCompletedEvent event = published.getValue();
        assertThat(event.jobId()).isEqualTo(id);
        assertThat(event.workerId()).isEqualTo(WORKER_ID);
        assertThat(event.status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(event.result()).isEqualTo("S3://BUCKET/ONE");
        assertThat(event.completedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("a job that loses the claim is not run and not reported")
    void aLostClaimIsNotReported() {
        UUID id = UUID.randomUUID();
        given(claims.findPendingIds(5)).willReturn(List.of(id));
        given(claims.claim(id, WORKER_ID)).willReturn(false);

        poller().poll();

        verifyNoInteractions(publisher);
    }

    /**
     * No other test in this repository produces a FAILED job, so this is the only proof
     * that the failure branch reports at all.
     */
    @Test
    @DisplayName("work that throws is reported FAILED with the failure message")
    void workThatThrowsIsReportedFailed() {
        UUID id = UUID.randomUUID();
        given(claims.findPendingIds(5)).willReturn(List.of(id));
        given(claims.claim(id, WORKER_ID)).willReturn(true);
        // A claimed row with no payload reference. The real runner rejects it.
        given(claims.findPayloadRef(id)).willReturn(null);

        poller().poll();

        verify(publisher, times(1)).publish(published.capture());
        JobCompletedEvent event = published.getValue();
        assertThat(event.jobId()).isEqualTo(id);
        assertThat(event.status()).isEqualTo(JobStatus.FAILED);
        assertThat(event.result()).isEqualTo("payloadRef is blank");
    }

    @Test
    @DisplayName("a failure with no message is not reported as the text \"null\"")
    void aFailureWithNoMessageIsDescribedByItsType() throws Exception {
        UUID id = UUID.randomUUID();
        given(claims.findPendingIds(5)).willReturn(List.of(id));
        given(claims.claim(id, WORKER_ID)).willReturn(true);

        JobRunner runner = org.mockito.Mockito.mock(JobRunner.class);
        given(runner.run(any())).willThrow(new IllegalStateException());

        pollerWith(runner).poll();

        verify(publisher).publish(published.capture());
        assertThat(published.getValue().status()).isEqualTo(JobStatus.FAILED);
        assertThat(published.getValue().result())
                .isNotEqualTo("null")
                .contains(IllegalStateException.class.getName());
    }

    /**
     * The regression this poller was fixed for.
     *
     * <p>The report used to sit inside the try block that catches a failed job. A
     * publish that threw was therefore read as the work having failed, and the worker
     * answered by publishing a second event: FAILED, carrying the broker's error text as
     * the job result. A job that had succeeded ended up recorded as a failure whose
     * result was a stack message.
     *
     * <p>Against that older shape this test fails twice over: {@code publish} is called
     * two times, and the second call carries {@code FAILED}.
     */
    @Test
    @DisplayName("a publish that throws does not turn a succeeded job into a FAILED report")
    void aBrokenPublishNeverReportsFailure() {
        UUID id = UUID.randomUUID();
        given(claims.findPendingIds(5)).willReturn(List.of(id));
        given(claims.claim(id, WORKER_ID)).willReturn(true);
        given(claims.findPayloadRef(id)).willReturn("s3://bucket/one");

        willThrow(new IllegalStateException("Failed to send to topic pubfleet.job.completed"))
                .given(publisher).publish(any());

        // The poll swallows the reporting failure so the rest of the batch still runs.
        assertThatCode(() -> poller().poll()).doesNotThrowAnyException();

        verify(publisher, times(1)).publish(published.capture());
        assertThat(published.getValue().status())
                .as("the work succeeded, so the only outcome offered must be SUCCEEDED")
                .isEqualTo(JobStatus.SUCCEEDED);
        assertThat(published.getAllValues())
                .as("no outcome may carry a broker error as the job result")
                .noneMatch(event -> event.status() == JobStatus.FAILED);
    }

    @Test
    @DisplayName("one job with a broken report does not stop the rest of the batch")
    void abatchContinuesAfterABrokenReport() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        given(claims.findPendingIds(5)).willReturn(List.of(first, second));
        given(claims.claim(any(), any())).willReturn(true);
        given(claims.findPayloadRef(any())).willReturn("s3://bucket/x");

        willThrow(new IllegalStateException("broker down"))
                .willDoNothing()
                .given(publisher).publish(any());

        poller().poll();

        verify(publisher, times(2)).publish(published.capture());
        assertThat(published.getAllValues()).extracting(JobCompletedEvent::jobId)
                .containsExactly(first, second);
    }

    @Test
    @DisplayName("an empty poll touches neither the claim nor the broker")
    void anEmptyPollDoesNothing() {
        given(claims.findPendingIds(5)).willReturn(List.of());

        poller().poll();

        verify(claims, never()).claim(any(), any());
        verifyNoInteractions(publisher);
    }
}
