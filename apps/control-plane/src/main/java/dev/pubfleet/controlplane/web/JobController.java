package dev.pubfleet.controlplane.web;

import dev.pubfleet.contracts.CreateJobRequest;
import dev.pubfleet.contracts.CreateJobResponse;
import dev.pubfleet.contracts.JobPage;
import dev.pubfleet.contracts.JobView;
import dev.pubfleet.controlplane.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * The frozen HTTP surface of unit 00.
 *
 * <p>Only the types from {@code libs/contracts} appear in these signatures. The JPA
 * entity never does, and ArchitectureTest fails the build if it ever starts to.
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobs;

    public JobController(JobService jobs) {
        this.jobs = jobs;
    }

    /**
     * Takes a job and answers at once. 202, not 201: the work has not run yet, so
     * there is no finished thing to point at.
     */
    @PostMapping
    public ResponseEntity<CreateJobResponse> create(@RequestBody CreateJobRequest request) {
        CreateJobResponse response = jobs.create(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/jobs/" + response.jobId()))
                .body(response);
    }

    /**
     * One page of jobs, newest first.
     *
     * @param after the {@code nextCursor} of the page before, or nothing for page one
     */
    @GetMapping
    public JobPage list(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "after", required = false) UUID after) {
        return jobs.list(limit, after);
    }

    /** One job, or a 404 problem detail. */
    @GetMapping("/{id}")
    public JobView get(@PathVariable("id") UUID id) {
        return jobs.get(id);
    }
}
