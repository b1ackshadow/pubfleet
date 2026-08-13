package dev.pubfleet.worker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The trivial work of unit 00. It is trivial, but the walking skeleton asserts its
 * output end to end, so the transform has to be exactly this one.
 */
class JobRunnerTest {

    private final JobRunner runner = new JobRunner(Duration.ZERO);

    @Test
    @DisplayName("the payload reference comes back in upper case")
    void itUpperCasesThePayloadReference() throws Exception {
        assertThat(runner.run("s3://bucket/walking-skeleton"))
                .isEqualTo("S3://BUCKET/WALKING-SKELETON");
    }

    /**
     * The root locale, not the default one. Under a Turkish default locale a default
     * {@code toUpperCase} turns "i" into a dotted capital I, and the result the console
     * shows would then depend on the machine the worker runs on.
     */
    @Test
    @DisplayName("the case change does not follow the default locale")
    void itUpperCasesInTheRootLocale() throws Exception {
        assertThat(runner.run("s3://bucket/invoices")).isEqualTo("S3://BUCKET/INVOICES");
    }

    @Test
    @DisplayName("a blank payload reference is rejected")
    void itRejectsABlankPayloadReference() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> runner.run("   "))
                .withMessage("payloadRef is blank");
    }

    @Test
    @DisplayName("a missing payload reference is rejected")
    void itRejectsAMissingPayloadReference() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> runner.run(null))
                .withMessage("payloadRef is blank");
    }

    @Test
    @DisplayName("the work waits for the configured delay")
    void itWaitsForTheConfiguredDelay() throws Exception {
        JobRunner slow = new JobRunner(Duration.ofMillis(120));

        long start = System.nanoTime();
        slow.run("s3://bucket/x");
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(100);
    }
}
