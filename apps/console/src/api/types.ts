/**
 * The wire contract with the control plane. Unit 00 freezes these shapes.
 * Do not widen a field to `string` when the server sends a closed set of values.
 */

/** Every legal job status, in lifecycle order. */
export const JOB_STATUSES = ['PENDING', 'CLAIMED', 'SUCCEEDED', 'FAILED'] as const

/**
 * A closed union, not a bare string. A new status becomes a compile error at
 * every switch and every lookup table that maps a status to something.
 */
export type JobStatus = (typeof JOB_STATUSES)[number]

/** Narrows an unknown value that came off the wire to a known status. */
export function isJobStatus(value: unknown): value is JobStatus {
  return (
    typeof value === 'string' && (JOB_STATUSES as readonly string[]).includes(value)
  )
}

export interface Job {
  id: string
  supplierId: string
  payloadRef: string
  status: JobStatus
  workerId: string | null
  result: string | null
  createdAt: string
  updatedAt: string
}

/** GET /api/jobs */
export interface JobPage {
  items: Job[]
  nextCursor: string | null
}

/** POST /api/jobs request body. */
export interface CreateJobRequest {
  supplierId: string
  payloadRef: string
}

/** POST /api/jobs 202 response. A new job is always PENDING. */
export interface CreateJobResponse {
  jobId: string
  status: Extract<JobStatus, 'PENDING'>
}

/** RFC 9457 problem detail, sent as application/problem+json. */
export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail: string
}
