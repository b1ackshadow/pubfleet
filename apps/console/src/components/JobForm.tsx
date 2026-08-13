import { useState } from 'react'
import type { FormEvent } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useCreateJobMutation } from '../api/queries'
import { ApiError } from '../api/client'

/** Reads the operator-facing message out of a failed submit. */
function errorMessage(error: Error): string {
  return error instanceof ApiError ? (error.problem?.detail ?? error.message) : error.message
}

export function JobForm() {
  const [supplierId, setSupplierId] = useState('')
  const [payloadRef, setPayloadRef] = useState('')
  const mutation = useCreateJobMutation()

  const canSubmit =
    supplierId.trim().length > 0 && payloadRef.trim().length > 0 && !mutation.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (!canSubmit) return

    mutation.mutate(
      { supplierId: supplierId.trim(), payloadRef: payloadRef.trim() },
      {
        onSuccess: () => {
          setPayloadRef('')
        },
      },
    )
  }

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Typography variant="h6" component="h2" gutterBottom>
        Submit a job
      </Typography>

      <Box component="form" data-testid="submit-form" onSubmit={handleSubmit} noValidate>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start">
          <TextField
            label="Supplier ID"
            value={supplierId}
            onChange={(event) => {
              setSupplierId(event.target.value)
            }}
            slotProps={{ htmlInput: { 'data-testid': 'supplier-input' } }}
            size="small"
            fullWidth
          />
          <TextField
            label="Payload ref"
            value={payloadRef}
            onChange={(event) => {
              setPayloadRef(event.target.value)
            }}
            slotProps={{ htmlInput: { 'data-testid': 'payload-input' } }}
            size="small"
            fullWidth
          />
          <Button
            type="submit"
            variant="contained"
            data-testid="submit-button"
            disabled={!canSubmit}
            sx={{ flexShrink: 0 }}
          >
            Submit
          </Button>
        </Stack>
      </Box>

      {mutation.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {errorMessage(mutation.error)}
        </Alert>
      )}
    </Paper>
  )
}
