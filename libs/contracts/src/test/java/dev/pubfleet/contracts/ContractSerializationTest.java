package dev.pubfleet.contracts;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The console and the worker are built against these shapes in parallel. A silent
 * rename here breaks both. This test pins the JSON field names and proves every wire
 * type survives a round trip.
 *
 * <p>The mapper is configured the way Spring Boot configures its own: ISO-8601 dates,
 * not epoch numbers.
 */
class ContractSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void createJobRequestRoundTrips() throws Exception {
        var original = new CreateJobRequest("supplier-7", "s3://bucket/key.xml");

        var json = mapper.writeValueAsString(original);
        assertThat(json).contains("\"supplierId\"", "\"payloadRef\"");

        assertThat(mapper.readValue(json, CreateJobRequest.class)).isEqualTo(original);
    }

    @Test
    void createJobRequestReadsTheFrozenBody() throws Exception {
        var body = """
                {"supplierId":"supplier-7","payloadRef":"s3://bucket/key.xml"}
                """;

        var parsed = mapper.readValue(body, CreateJobRequest.class);

        assertThat(parsed.supplierId()).isEqualTo("supplier-7");
        assertThat(parsed.payloadRef()).isEqualTo("s3://bucket/key.xml");
    }

    @Test
    void createJobResponseRoundTrips() throws Exception {
        var original = new CreateJobResponse(UUID.randomUUID(), JobStatus.PENDING);

        var json = mapper.writeValueAsString(original);
        assertThat(json).contains("\"jobId\"").contains("\"status\":\"PENDING\"");

        assertThat(mapper.readValue(json, CreateJobResponse.class)).isEqualTo(original);
    }

    @Test
    void jobViewRoundTripsWithEveryFieldSet() throws Exception {
        var original = new JobView(
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "supplier-7",
                "s3://bucket/key.xml",
                JobStatus.SUCCEEDED,
                "worker-1",
                "S3://BUCKET/KEY.XML",
                Instant.parse("2026-08-13T10:15:30Z"),
                Instant.parse("2026-08-13T10:15:31Z"));

        var json = mapper.writeValueAsString(original);
        assertThat(json).contains(
                "\"id\"", "\"supplierId\"", "\"payloadRef\"", "\"status\"",
                "\"workerId\"", "\"result\"", "\"createdAt\"", "\"updatedAt\"");
        assertThat(json).contains("\"createdAt\":\"2026-08-13T10:15:30Z\"");

        assertThat(mapper.readValue(json, JobView.class)).isEqualTo(original);
    }

    @Test
    void jobViewRoundTripsWithTheNullableFieldsUnset() throws Exception {
        var original = new JobView(
                UUID.randomUUID(), "supplier-7", "ref", JobStatus.PENDING,
                null, null, Instant.parse("2026-08-13T10:15:30Z"),
                Instant.parse("2026-08-13T10:15:30Z"));

        var json = mapper.writeValueAsString(original);
        assertThat(json).contains("\"workerId\":null", "\"result\":null");

        assertThat(mapper.readValue(json, JobView.class)).isEqualTo(original);
    }

    @Test
    void jobPageRoundTripsAndKeepsANullCursorOnTheLastPage() throws Exception {
        var job = new JobView(
                UUID.randomUUID(), "supplier-7", "ref", JobStatus.CLAIMED,
                "worker-1", null, Instant.parse("2026-08-13T10:15:30Z"),
                Instant.parse("2026-08-13T10:15:30Z"));
        var lastPage = new JobPage(List.of(job), null);

        var json = mapper.writeValueAsString(lastPage);
        assertThat(json).contains("\"items\"", "\"nextCursor\":null");

        assertThat(mapper.readValue(json, JobPage.class)).isEqualTo(lastPage);
    }

    @Test
    void jobPageRoundTripsWithACursor() throws Exception {
        var cursor = UUID.randomUUID();
        var page = new JobPage(List.of(), cursor);

        var restored = mapper.readValue(mapper.writeValueAsString(page), JobPage.class);

        assertThat(restored.nextCursor()).isEqualTo(cursor);
        assertThat(restored.items()).isEmpty();
    }

    @Test
    void jobCompletedEventRoundTrips() throws Exception {
        var original = new JobCompletedEvent(
                UUID.randomUUID(),
                "worker-1",
                JobStatus.FAILED,
                "payloadRef was blank",
                Instant.parse("2026-08-13T10:15:31Z"));

        var json = mapper.writeValueAsString(original);
        assertThat(json).contains(
                "\"jobId\"", "\"workerId\"", "\"status\"", "\"result\"", "\"completedAt\"");

        assertThat(mapper.readValue(json, JobCompletedEvent.class)).isEqualTo(original);
    }

    @Test
    void everyStatusNameSurvivesTheWire() throws Exception {
        for (JobStatus status : JobStatus.values()) {
            var json = mapper.writeValueAsString(status);

            assertThat(json).isEqualTo("\"" + status.name() + "\"");
            assertThat(mapper.readValue(json, JobStatus.class)).isEqualTo(status);
        }
    }
}
