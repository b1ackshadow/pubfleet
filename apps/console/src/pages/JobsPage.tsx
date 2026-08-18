import Alert from '@mui/material/Alert'
import Container from '@mui/material/Container'
import LinearProgress from '@mui/material/LinearProgress'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { JobForm } from '../components/JobForm'
import { JobTable } from '../components/JobTable'
import { useJobsQuery } from '../api/queries'
import { ApiError } from '../api/client'

function errorMessage(error: Error): string {
  return error instanceof ApiError ? (error.problem?.detail ?? error.message) : error.message
}

export function JobsPage() {
  const jobsQuery = useJobsQuery()

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Typography variant="h4" component="h1">
          pubfleet operator console
        </Typography>

        <JobForm />

        <Stack spacing={1}>
          <Typography variant="h6" component="h2">
            Jobs
          </Typography>

          {jobsQuery.isPending && <LinearProgress />}

          {jobsQuery.isError && (
            <Alert severity="error">{errorMessage(jobsQuery.error)}</Alert>
          )}

          {jobsQuery.data !== undefined && <JobTable jobs={jobsQuery.data.items} />}
        </Stack>
      </Stack>
    </Container>
  )
}
