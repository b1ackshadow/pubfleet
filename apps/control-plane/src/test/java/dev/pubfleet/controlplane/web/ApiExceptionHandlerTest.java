package dev.pubfleet.controlplane.web;

import dev.pubfleet.controlplane.domain.InvalidJobRequestException;
import dev.pubfleet.controlplane.domain.JobNotFoundException;
import dev.pubfleet.controlplane.service.JobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The console reads one error shape and one only. Its guard requires a string
 * {@code title}, a string {@code detail} and a number {@code status}; anything else is
 * dropped and the operator is shown nothing useful.
 *
 * <p>Four of these errors never reach a controller. Spring raises them while it is
 * routing, converting a path variable, or reading a body, so they are the ones that used
 * to leave as Boot's default body. They are the reason this advice extends
 * {@code ResponseEntityExceptionHandler}.
 */
@WebMvcTest(JobController.class)
class ApiExceptionHandlerTest {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JobService jobs;

    @Test
    @DisplayName("a missing job is a 404 problem detail")
    void missingJobIsAProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();
        given(jobs.get(id)).willThrow(new JobNotFoundException(id));

        mvc.perform(get("/api/jobs/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").value("Job not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.jobId").value(id.toString()));
    }

    @Test
    @DisplayName("a path variable that is not a UUID is a 400 problem detail")
    void badPathVariableIsAProblemDetail() throws Exception {
        mvc.perform(get("/api/jobs/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    @DisplayName("a body Jackson cannot read is a 400 problem detail")
    void malformedBodyIsAProblemDetail() throws Exception {
        mvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    @DisplayName("a method the route does not have is a 405 problem detail")
    void wrongMethodIsAProblemDetail() throws Exception {
        mvc.perform(delete("/api/jobs"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    @DisplayName("a blank field is a 400 that names the field")
    void blankFieldIsAProblemDetail() throws Exception {
        willThrow(new InvalidJobRequestException("supplierId", "supplierId must not be blank"))
                .given(jobs).create(any());

        mvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":\" \",\"payloadRef\":\"s3://x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("supplierId must not be blank"))
                .andExpect(jsonPath("$.field").value("supplierId"));
    }

    /**
     * The regression that matters most here. An {@code IllegalArgumentException} used to
     * be mapped straight to 400 with its own message in {@code detail}, so an internal
     * bug was reported to the caller as a client mistake, wearing an internal string.
     */
    @Test
    @DisplayName("an internal IllegalArgumentException is a 500 and leaks no message")
    void internalIllegalArgumentIsAServerError() throws Exception {
        willThrow(new IllegalArgumentException("jobs.status column is not a JobStatus"))
                .given(jobs).create(any());

        mvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":\"acme\",\"payloadRef\":\"s3://x\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail")
                        .value("The control plane could not complete the request."));
    }
}
