import Chip from '@mui/material/Chip'
import type { ChipProps } from '@mui/material/Chip'
import type { JobStatus } from '../api/types'

/**
 * One colour per status. The Record is exhaustive on purpose: a new status in
 * the union breaks this build until somebody picks its colour.
 */
const STATUS_COLOR: Record<JobStatus, NonNullable<ChipProps['color']>> = {
  PENDING: 'default',
  CLAIMED: 'info',
  SUCCEEDED: 'success',
  FAILED: 'error',
}

export interface JobStatusChipProps {
  status: JobStatus
}

export function JobStatusChip({ status }: JobStatusChipProps) {
  return (
    <Chip
      data-testid="job-status"
      label={status}
      color={STATUS_COLOR[status]}
      size="small"
      variant="filled"
    />
  )
}
