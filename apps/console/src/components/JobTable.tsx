import Paper from '@mui/material/Paper'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { JobStatusChip } from './JobStatusChip'
import type { Job } from '../api/types'

/** The short id the operator reads. The full id stays in the row title. */
function shortId(id: string): string {
  return id.slice(0, 8)
}

const MONO = { fontFamily: 'monospace' } as const

export interface JobTableProps {
  jobs: Job[]
}

export function JobTable({ jobs }: JobTableProps) {
  if (jobs.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography color="text.secondary">No jobs yet. Submit one above.</Typography>
      </Paper>
    )
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small" aria-label="jobs">
        <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Supplier</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Worker</TableCell>
            <TableCell>Result</TableCell>
            <TableCell>Updated</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {jobs.map((job) => (
            <TableRow key={job.id} data-testid="job-row" hover title={job.id}>
              <TableCell sx={MONO}>{shortId(job.id)}</TableCell>
              <TableCell>{job.supplierId}</TableCell>
              <TableCell>
                <JobStatusChip status={job.status} />
              </TableCell>
              <TableCell sx={MONO}>{job.workerId ?? '—'}</TableCell>
              <TableCell>{job.result ?? '—'}</TableCell>
              <TableCell sx={MONO}>{job.updatedAt}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
