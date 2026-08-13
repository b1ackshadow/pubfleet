package dev.pubfleet.worker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The worker boots, wires its own beans, and reads its own properties.
 *
 * <p>No container starts. The two beans that talk to the outside are replaced, and the
 * data source is never asked for a connection, so this stays a wiring proof and nothing
 * more. It catches the failures a unit test cannot see: a property prefix that no longer
 * binds, a bean the context cannot build, a scheduled method Spring will not accept.
 *
 * <p>The poll delay is set to an hour so the scheduler's first run is the only one, and
 * the claim repository is a double, so that run finds no work and ends at once.
 */
@SpringBootTest(properties = {
        "pubfleet.worker.id=worker-context-test",
        "pubfleet.worker.batch-size=7",
        "pubfleet.worker.work-delay=5ms",
        "pubfleet.worker.poll-delay=1h",
        "spring.datasource.url=jdbc:postgresql://localhost:1/pubfleet-never-connected",
        "spring.kafka.bootstrap-servers=localhost:1"
})
class WorkerApplicationTest {

    @MockitoBean
    private JobClaimRepository claims;

    @MockitoBean
    private JobCompletedPublisher publisher;

    @Autowired
    private JobPoller poller;

    @Autowired
    private JobRunner runner;

    @Autowired
    private WorkerProperties properties;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("the context loads with the poller, the runner and the clock wired")
    void theContextLoads() {
        assertThat(poller).isNotNull();
        assertThat(runner).isNotNull();
        assertThat(clock).isNotNull();
    }

    @Test
    @DisplayName("the pubfleet.worker properties bind")
    void thePropertiesBind() {
        assertThat(properties.id()).isEqualTo("worker-context-test");
        assertThat(properties.batchSize()).isEqualTo(7);
        assertThat(properties.workDelay()).isEqualTo(Duration.ofMillis(5));
    }
}
