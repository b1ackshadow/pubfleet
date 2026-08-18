import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { JobTable } from './JobTable'
import { JOB_STATUSES } from '../api/types'
import type { Job, JobStatus } from '../api/types'

function job(status: JobStatus, index: number): Job {
  const claimed = status !== 'PENDING'
  return {
    id: `a3f1c2d4-0000-4000-8000-00000000000${index}`,
    supplierId: `supplier-${index}`,
    payloadRef: `s3://drop/batch-${index}.xml`,
    status,
    workerId: claimed ? `worker-${index}` : null,
    result: status === 'SUCCEEDED' ? 'ok' : status === 'FAILED' ? 'boom' : null,
    createdAt: '2026-08-13T09:00:00Z',
    updatedAt: `2026-08-13T09:0${index}:00Z`,
  }
}

/** The MUI colour class each status must land on. All four differ. */
const EXPECTED_COLOR_CLASS: Record<JobStatus, string> = {
  PENDING: 'MuiChip-colorDefault',
  CLAIMED: 'MuiChip-colorInfo',
  SUCCEEDED: 'MuiChip-colorSuccess',
  FAILED: 'MuiChip-colorError',
}

describe('JobTable', () => {
  it('renders one row per job', () => {
    const jobs = JOB_STATUSES.map((status, index) => job(status, index))
    render(<JobTable jobs={jobs} />)

    expect(screen.getAllByTestId('job-row')).toHaveLength(JOB_STATUSES.length)
  })

  it('renders a chip for every status, each with its own colour', () => {
    const jobs = JOB_STATUSES.map((status, index) => job(status, index))
    render(<JobTable jobs={jobs} />)

    const chips = screen.getAllByTestId('job-status')
    expect(chips).toHaveLength(JOB_STATUSES.length)

    JOB_STATUSES.forEach((status, index) => {
      const chip = chips[index]
      expect(chip).toBeDefined()
      expect(chip).toHaveTextContent(status)
      expect(chip).toHaveClass(EXPECTED_COLOR_CLASS[status])
    })

    const colours = new Set(Object.values(EXPECTED_COLOR_CLASS))
    expect(colours.size).toBe(JOB_STATUSES.length)
  })

  it('shows the first eight characters of the id, the worker, the result and the update time', () => {
    render(<JobTable jobs={[job('SUCCEEDED', 2)]} />)

    const row = screen.getByTestId('job-row')
    expect(row).toHaveTextContent('a3f1c2d4')
    expect(row).not.toHaveTextContent('a3f1c2d4-0000')
    expect(row).toHaveTextContent('supplier-2')
    expect(row).toHaveTextContent('worker-2')
    expect(row).toHaveTextContent('ok')
    expect(row).toHaveTextContent('2026-08-13T09:02:00Z')
  })

  it('renders a dash when the worker and the result are null', () => {
    render(<JobTable jobs={[job('PENDING', 1)]} />)

    const cells = screen.getByTestId('job-row').querySelectorAll('td')
    expect(cells[3]).toHaveTextContent('—')
    expect(cells[4]).toHaveTextContent('—')
  })

  it('tells the operator when there are no jobs', () => {
    render(<JobTable jobs={[]} />)

    expect(screen.queryAllByTestId('job-row')).toHaveLength(0)
    expect(screen.getByText(/no jobs yet/i)).toBeInTheDocument()
  })
})
