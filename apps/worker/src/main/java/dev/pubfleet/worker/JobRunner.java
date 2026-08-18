package dev.pubfleet.worker;

import java.time.Duration;
import java.util.Locale;

/**
 * The trivial work of unit 00.
 *
 * <p>It puts the payload reference into upper case and waits. The point of the unit is
 * the path between the processes, not the work. Real work arrives with a later unit.
 */
public class JobRunner {

    private final Duration workDelay;

    public JobRunner(Duration workDelay) {
        this.workDelay = workDelay;
    }

    /**
     * Runs the work and gives the result.
     *
     * @throws InterruptedException when the thread is stopped mid-work
     * @throws IllegalArgumentException when the payload reference is missing
     */
    public String run(String payloadRef) throws InterruptedException {
        if (payloadRef == null || payloadRef.isBlank()) {
            throw new IllegalArgumentException("payloadRef is blank");
        }

        Thread.sleep(workDelay.toMillis());

        return payloadRef.toUpperCase(Locale.ROOT);
    }
}
