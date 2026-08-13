package dev.pubfleet.controlplane.service;

import dev.pubfleet.contracts.CreateJobRequest;
import dev.pubfleet.contracts.CreateJobResponse;
import dev.pubfleet.contracts.JobCompletedEvent;
import dev.pubfleet.contracts.JobPage;
import dev.pubfleet.contracts.JobStatus;
import dev.pubfleet.contracts.JobView;
import dev.pubfleet.controlplane.domain.InvalidJobRequestException;
import dev.pubfleet.controlplane.domain.JobNotFoundException;
import dev.pubfleet.controlplane.domain.JobStateMachine;
import dev.pubfleet.controlplane.persistence.JobEntity;
import dev.pubfleet.controlplane.persistence.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Every read and write of a job row goes through here. */
@Service
public class JobService {

    /** The largest page the API will return, whatever the caller asks for. */
    public static final int MAX_PAGE_SIZE = 200;

    /** The page size when the caller gives none. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobs;
    private final Clock clock;

    public JobService(JobRepository jobs, Clock clock) {
        this.jobs = jobs;
        this.clock = clock;
    }

    /** Writes a PENDING row. A worker picks it up on its next poll. */
    @Transactional
    public CreateJobResponse create(CreateJobRequest request) {
        // The contract record carries no validation annotations, because libs/contracts
        // holds wire shapes and nothing else. The guard belongs here instead.
        requireText(request.supplierId(), "supplierId");
        requireText(request.payloadRef(), "payloadRef");

        Instant now = clock.instant();
        JobEntity job = new JobEntity(
                UUID.randomUUID(),
                request.supplierId(),
                request.payloadRef(),
                JobStatus.PENDING,
                now,
                now);

        jobs.save(job);
        log.info("Accepted job {} from supplier {}", job.getId(), job.getSupplierId());

        return new CreateJobResponse(job.getId(), job.getStatus());
    }

    /** One page of jobs, newest first. */
    @Transactional(readOnly = true)
    public JobPage list(Integer limit, UUID after) {
        int pageSize = clampPageSize(limit);

        List<JobEntity> rows = (after == null)
                ? jobs.findFirstPage(pageSize)
                : jobs.findPageAfter(after, pageSize);

        List<JobView> items = rows.stream().map(JobService::toView).toList();
        // A full page means there may be more. A short page is the end.
        UUID nextCursor = (items.size() == pageSize && !items.isEmpty())
                ? items.getLast().id()
                : null;

        return new JobPage(items, nextCursor);
    }

    /** One job. */
    @Transactional(readOnly = true)
    public JobView get(UUID id) {
        return jobs.findById(id).map(JobService::toView)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    /**
     * Writes the terminal state reported by a worker.
     *
     * <p>This is the only place a job reaches SUCCEEDED or FAILED. The worker publishes
     * an outcome. The control plane decides whether the outcome is a legal move, and
     * {@link JobStateMachine} throws when it is not.
     */
    @Transactional
    public JobView applyCompletion(JobCompletedEvent event) {
        JobEntity job = jobs.findById(event.jobId())
                .orElseThrow(() -> new JobNotFoundException(event.jobId()));

        JobStateMachine.assertLegal(job.getId(), job.getStatus(), event.status());

        job.setStatus(event.status());
        job.setResult(event.result());
        job.setWorkerId(event.workerId());
        job.setUpdatedAt(clock.instant());
        jobs.save(job);

        log.info("Job {} finished as {} on worker {}",
                job.getId(), job.getStatus(), job.getWorkerId());

        return toView(job);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidJobRequestException(field, field + " must not be blank");
        }
    }

    private static int clampPageSize(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private static JobView toView(JobEntity job) {
        return new JobView(
                job.getId(),
                job.getSupplierId(),
                job.getPayloadRef(),
                job.getStatus(),
                job.getWorkerId(),
                job.getResult(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
