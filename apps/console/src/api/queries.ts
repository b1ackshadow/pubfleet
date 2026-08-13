import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query'
import { createJob, listJobs } from './client'
import type { CreateJobRequest, CreateJobResponse, JobPage } from './types'
import { usePageVisible } from '../hooks/usePageVisible'

/** How often the job list refetches while the tab is visible, in milliseconds. */
export const JOB_POLL_INTERVAL_MS = 1000

export const jobKeys = {
  all: ['jobs'] as const,
  list: (limit: number) => ['jobs', 'list', limit] as const,
}

/**
 * Polls the job list every second. Polling stops while the tab is hidden, so a
 * background tab does not hold the control plane under load.
 */
export function useJobsQuery(limit = 50): UseQueryResult<JobPage, Error> {
  const visible = usePageVisible()

  return useQuery({
    queryKey: jobKeys.list(limit),
    queryFn: () => listJobs({ limit }),
    refetchInterval: visible ? JOB_POLL_INTERVAL_MS : false,
    refetchIntervalInBackground: false,
  })
}

/** Submits a job, then invalidates the list so the new row appears at once. */
export function useCreateJobMutation(): UseMutationResult<
  CreateJobResponse,
  Error,
  CreateJobRequest
> {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createJob,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: jobKeys.all })
    },
  })
}
