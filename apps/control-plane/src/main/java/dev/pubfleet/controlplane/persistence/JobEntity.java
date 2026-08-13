package dev.pubfleet.controlplane.persistence;

import dev.pubfleet.contracts.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The job row. This type never leaves the control plane: the web layer answers with
 * {@code dev.pubfleet.contracts.JobView}. ArchitectureTest holds that line.
 *
 * <p>No Lombok. Plain fields, an explicit constructor, and plain accessors.
 */
@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private String supplierId;

    @Column(name = "payload_ref", nullable = false)
    private String payloadRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "result")
    private String result;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** For Hibernate. */
    protected JobEntity() {
    }

    public JobEntity(UUID id, String supplierId, String payloadRef, JobStatus status,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.supplierId = supplierId;
        this.payloadRef = payloadRef;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getPayloadRef() {
        return payloadRef;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
