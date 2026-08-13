package dev.pubfleet.controlplane;

import dev.pubfleet.contracts.CreateJobRequest;
import dev.pubfleet.contracts.CreateJobResponse;
import dev.pubfleet.contracts.JobCompletedEvent;
import dev.pubfleet.contracts.JobStatus;
import dev.pubfleet.contracts.JobView;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The one end-to-end proof of unit 00. It walks the whole path and fails if any hop
 * breaks: HTTP submit, Postgres write, worker claim, Kafka publish, control-plane
 * consume, terminal state.
 *
 * <p><b>Why the worker application does not boot here.</b> The worker is a separate
 * deployable and a separate Maven module. Starting its Spring context inside this test
 * would turn two processes back into one, which is the exact thing unit 00 exists to
 * prove is not happening. Instead the test drives the worker's two behaviours directly
 * against the same real containers:
 *
 * <ol>
 *   <li>it runs the worker's own claim statement, copied verbatim and checked against
 *       the worker source by {@link #assertClaimStatementsMatchTheWorkerSource()}, so a
 *       drifted copy fails the build instead of passing a stale proof;</li>
 *   <li>it publishes a real {@link JobCompletedEvent} through a producer configured
 *       exactly like the worker's, so what lands on the broker is the wire format and
 *       not a mock of it.</li>
 * </ol>
 *
 * <p>Everything after the publish is the real control plane: the real Kafka listener,
 * the real state machine, the real transaction. Assertions read the persisted row back
 * with plain SQL, not through JPA, so a value sitting in the persistence context cannot
 * be mistaken for a value written to the database.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobLifecycleIT {

    /** The image the compose file uses. A different major version proves nothing. */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    /**
     * KRaft, single node, same image and tag as docker-compose.yml.
     *
     * <p>The listener override is not a preference. Testcontainers 1.21.4 binds every
     * listener to {@code 0.0.0.0}, including the controller. Kafka 3.9 then derives the
     * advertised controller endpoint from that bind address, finds the meta-address
     * {@code 0.0.0.0}, and refuses to format its storage. The compose file avoids the
     * same trap by writing {@code CONTROLLER://:9093} rather than a bind-all address.
     * A single node is its own quorum, so binding the controller to localhost inside
     * the container matches {@code controller.quorum.voters=1@localhost:9094} exactly.
     */
    @Container
    static final org.testcontainers.kafka.KafkaContainer KAFKA =
            new org.testcontainers.kafka.KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"))
                    .withEnv("KAFKA_LISTENERS",
                            "PLAINTEXT://0.0.0.0:9092,BROKER://0.0.0.0:9093,CONTROLLER://localhost:9094");

    /**
     * The worker's candidate read, from
     * {@code dev.pubfleet.worker.JobClaimRepository#findPendingIds}.
     */
    private static final String WORKER_PENDING_SQL = """
            SELECT id FROM jobs
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :limit
            """;

    /**
     * The worker's claim, from {@code dev.pubfleet.worker.JobClaimRepository#claim}.
     * The {@code AND status='PENDING'} clause is the compare-and-set that settles the
     * race between two workers.
     */
    private static final String WORKER_CLAIM_SQL =
            "UPDATE jobs SET status='CLAIMED', worker_id=:workerId, updated_at=now() "
                    + "WHERE id=:id AND status='PENDING'";

    /** The worker source this test copies its SQL from, relative to this module. */
    private static final Path WORKER_CLAIM_SOURCE = Path.of(
            "..", "worker", "src", "main", "java", "dev", "pubfleet", "worker",
            "JobClaimRepository.java");

    private static final String WORKER_ID = "worker-it";
    private static final Duration TERMINAL_TIMEOUT = Duration.ofSeconds(30);

    private DefaultKafkaProducerFactory<String, JobCompletedEvent> workerProducerFactory;
    private KafkaTemplate<String, JobCompletedEvent> workerProducer;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private KafkaListenerEndpointRegistry listeners;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    /**
     * A producer built from the worker's {@code application.yml}: string key, Spring
     * JSON value, {@code acks=all}, and no type header, because the control plane reads
     * a fixed contract type. Change either side and this test stops passing, which is
     * the point of publishing rather than mocking.
     */
    @BeforeEach
    void startWorkerProducer() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // The serializers go in as classes, so the Kafka client builds and configures
        // them itself. That is how the worker gets its own, and it is what puts the
        // contract on the wire in the shape the control plane's deserializer expects.
        workerProducerFactory = new DefaultKafkaProducerFactory<>(config);
        workerProducer = new KafkaTemplate<>(workerProducerFactory);
    }

    @AfterEach
    void stopWorkerProducer() {
        workerProducerFactory.destroy();
    }

    /**
     * An event published before the listener holds its partition would be read anyway,
     * because the group resets to the earliest offset. Waiting anyway keeps a failure
     * about the chain from being read as a timing accident.
     */
    @BeforeEach
    void awaitListenerAssignment() {
        listeners.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(container, 1));
    }

    @Test
    void aSubmittedJobWalksTheWholeChainToItsTerminalState() throws Exception {
        assertClaimStatementsMatchTheWorkerSource();

        // 1. HTTP submit.
        ResponseEntity<CreateJobResponse> accepted = http.postForEntity(
                "/api/jobs",
                new CreateJobRequest("supplier-it", "s3://bucket/walking-skeleton"),
                CreateJobResponse.class);

        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(accepted.getBody()).isNotNull();
        UUID jobId = accepted.getBody().jobId();
        assertThat(jobId).isNotNull();
        assertThat(accepted.getBody().status()).isEqualTo(JobStatus.PENDING);
        assertThat(accepted.getHeaders().getLocation())
                .hasToString("/api/jobs/" + jobId);

        // 2. Postgres write. Read the row itself, not the response that claimed it.
        assertThat(persistedRow(jobId))
                .containsEntry("status", "PENDING")
                .containsEntry("worker_id", null)
                .containsEntry("result", null);

        // 3. Worker claim, through the worker's own statements.
        assertThat(pendingIdsAsTheWorkerSeesThem()).contains(jobId);
        assertThat(claimAsTheWorkerDoes(jobId))
                .as("the claim must win the row exactly once")
                .isTrue();
        assertThat(persistedRow(jobId))
                .containsEntry("status", "CLAIMED")
                .containsEntry("worker_id", WORKER_ID);

        // A second claim of the same row must lose. This is the compare-and-set that
        // makes the deliberately racy claim safe.
        assertThat(claimAsTheWorkerDoes(jobId))
                .as("a claim of an already claimed row must update no row")
                .isFalse();

        // 4. Kafka publish, in the worker's wire format on the worker's topic.
        String expectedResult = "s3://bucket/walking-skeleton".toUpperCase(Locale.ROOT);
        publishCompletion(jobId, JobStatus.SUCCEEDED, expectedResult, WORKER_ID);

        // 5. Control-plane consume, and 6. the terminal state, in the database.
        await().atMost(TERMINAL_TIMEOUT).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(persistedRow(jobId))
                        .containsEntry("status", "SUCCEEDED")
                        .containsEntry("result", expectedResult)
                        .containsEntry("worker_id", WORKER_ID));

        // The same state must be what the console would read back over HTTP.
        JobView served = http.getForObject("/api/jobs/" + jobId, JobView.class);
        assertThat(served.status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(served.result()).isEqualTo(expectedResult);
        assertThat(served.workerId()).isEqualTo(WORKER_ID);
    }

    /**
     * A repeat or a rogue completion must not rewrite a job that has already finished.
     *
     * <p>The control plane owns the terminal state, so the guard has to hold on the
     * consumer path and not only on the HTTP path. There is no acknowledgement to wait
     * for when an event is correctly ignored, so the test publishes a second, legal
     * completion for a different job behind it. The topic has one partition, so when
     * the later job reaches its terminal state the earlier event has certainly been
     * handled and rejected.
     */
    @Test
    void aCompletionForAFinishedJobIsRejectedAndChangesNothing() throws Exception {
        UUID finished = submitAndClaim("supplier-terminal", "s3://bucket/first");
        publishCompletion(finished, JobStatus.SUCCEEDED, "FIRST RESULT", WORKER_ID);
        awaitStatus(finished, "SUCCEEDED");

        // The illegal move: SUCCEEDED to FAILED, with values that would be obvious if
        // they ever landed.
        publishCompletion(finished, JobStatus.FAILED, "overwritten by a rogue worker",
                "rogue-worker");

        // The fence. Its terminal state proves the consumer has passed the event above.
        UUID fence = submitAndClaim("supplier-fence", "s3://bucket/fence");
        publishCompletion(fence, JobStatus.SUCCEEDED, "FENCE RESULT", WORKER_ID);
        awaitStatus(fence, "SUCCEEDED");

        assertThat(persistedRow(finished))
                .as("a terminal job must not be rewritten by a later completion")
                .containsEntry("status", "SUCCEEDED")
                .containsEntry("result", "FIRST RESULT")
                .containsEntry("worker_id", WORKER_ID);
    }

    // --- the worker's behaviour, driven directly ----------------------------------

    private List<UUID> pendingIdsAsTheWorkerSeesThem() {
        return jdbc.queryForList(WORKER_PENDING_SQL, Map.of("limit", 50), UUID.class);
    }

    private boolean claimAsTheWorkerDoes(UUID jobId) {
        return jdbc.update(WORKER_CLAIM_SQL, Map.of("workerId", WORKER_ID, "id", jobId)) == 1;
    }

    private void publishCompletion(UUID jobId, JobStatus status, String result, String workerId)
            throws Exception {
        JobCompletedEvent event =
                new JobCompletedEvent(jobId, workerId, status, result, Instant.now());

        // Keyed by the job id, and waited on, so a broker rejection fails the test here
        // rather than as a timeout somewhere later.
        workerProducer.send(JobCompletedEvent.TOPIC, jobId.toString(), event)
                .get(15, TimeUnit.SECONDS);
    }

    // --- helpers -------------------------------------------------------------------

    private UUID submitAndClaim(String supplierId, String payloadRef) {
        CreateJobResponse accepted = http.postForObject(
                "/api/jobs", new CreateJobRequest(supplierId, payloadRef),
                CreateJobResponse.class);

        assertThat(accepted).isNotNull();
        assertThat(claimAsTheWorkerDoes(accepted.jobId())).isTrue();

        return accepted.jobId();
    }

    private Map<String, Object> persistedRow(UUID jobId) {
        return jdbc.queryForMap(
                "SELECT status, worker_id, result FROM jobs WHERE id = :id",
                Map.of("id", jobId));
    }

    private void awaitStatus(UUID jobId, String status) {
        await().atMost(TERMINAL_TIMEOUT).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() ->
                        assertThat(persistedRow(jobId)).containsEntry("status", status));
    }

    /**
     * The SQL above is a copy, and a copy rots. This reads the worker source and fails
     * if the statements no longer match, so the test cannot keep proving a claim the
     * worker has stopped making. Quotes, string concatenation, and layout are removed
     * before the comparison; the statement itself is not.
     */
    private static void assertClaimStatementsMatchTheWorkerSource() throws IOException {
        Path source = Path.of(System.getProperty("user.dir")).resolve(WORKER_CLAIM_SOURCE);
        assertThat(source)
                .as("the worker source this test copies its SQL from")
                .isRegularFile();

        String worker = flatten(Files.readString(source, StandardCharsets.UTF_8));

        assertThat(worker)
                .as("the worker's candidate read has changed; update WORKER_PENDING_SQL")
                .contains(flatten(WORKER_PENDING_SQL));
        assertThat(worker)
                .as("the worker's claim has changed; update WORKER_CLAIM_SQL")
                .contains(flatten(WORKER_CLAIM_SQL));
    }

    private static String flatten(String text) {
        return text.replace("\"", " ").replace("+", " ").replaceAll("\\s+", " ").trim();
    }
}
