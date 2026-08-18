package dev.pubfleet.controlplane.service;

import dev.pubfleet.contracts.CreateJobRequest;
import dev.pubfleet.controlplane.domain.InvalidJobRequestException;
import dev.pubfleet.controlplane.persistence.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The guard on the create path.
 *
 * <p>The type it throws is the point. A plain {@code IllegalArgumentException} here
 * cannot be told apart from one raised by a library or by a bug in this application, and
 * the web layer would have to guess which of the two it is looking at.
 */
@ExtendWith(MockitoExtension.class)
class JobServiceValidationTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-08-13T09:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JobRepository jobs;

    @Test
    void aBlankSupplierIdIsRejectedBeforeAnyWrite() {
        JobService service = new JobService(jobs, FIXED);

        assertThatExceptionOfType(InvalidJobRequestException.class)
                .isThrownBy(() -> service.create(new CreateJobRequest("  ", "s3://x")))
                .withMessage("supplierId must not be blank")
                .satisfies(thrown -> org.assertj.core.api.Assertions
                        .assertThat(thrown.field()).isEqualTo("supplierId"));

        verifyNoInteractions(jobs);
    }

    @Test
    void aMissingPayloadRefIsRejectedBeforeAnyWrite() {
        JobService service = new JobService(jobs, FIXED);

        assertThatExceptionOfType(InvalidJobRequestException.class)
                .isThrownBy(() -> service.create(new CreateJobRequest("acme", null)))
                .withMessage("payloadRef must not be blank");

        verifyNoInteractions(jobs);
    }
}
