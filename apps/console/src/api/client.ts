import type {
  CreateJobRequest,
  CreateJobResponse,
  Job,
  JobPage,
  ProblemDetail,
} from './types'

/**
 * The base URL of the control plane.
 *
 * An empty value makes every request relative, which sends it through the Vite
 * dev server proxy. `.env.development` sets it empty for that reason.
 */
export const API_BASE_URL: string = (
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081'
).replace(/\/+$/, '')

/**
 * An error that carries the RFC 9457 problem detail when the server sent one.
 * The UI shows `detail` to the operator.
 */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  if (typeof value !== 'object' || value === null) return false
  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.title === 'string' &&
    typeof candidate.detail === 'string' &&
    typeof candidate.status === 'number'
  )
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail | null = null
  try {
    const body: unknown = await response.json()
    if (isProblemDetail(body)) problem = body
  } catch {
    // A body that is not JSON gives no problem detail. Fall back to the status.
  }
  const message = problem?.detail ?? `Request failed with status ${response.status}`
  return new ApiError(response.status, problem, message)
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init?.body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...init?.headers,
    },
  })

  if (!response.ok) throw await toApiError(response)
  return (await response.json()) as T
}

/** POST /api/jobs -> 202 Accepted. */
export function createJob(body: CreateJobRequest): Promise<CreateJobResponse> {
  return request<CreateJobResponse>('/api/jobs', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/** GET /api/jobs?limit=&after= */
export function listJobs(
  options: { limit?: number; after?: string | null } = {},
): Promise<JobPage> {
  const { limit = 50, after = null } = options
  const params = new URLSearchParams({ limit: String(limit) })
  if (after !== null) params.set('after', after)
  return request<JobPage>(`/api/jobs?${params.toString()}`)
}

/** GET /api/jobs/{id} */
export function getJob(id: string): Promise<Job> {
  return request<Job>(`/api/jobs/${encodeURIComponent(id)}`)
}
