import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { JobForm } from './JobForm'
import { jsonResponse, problemResponse, renderWithQuery } from '../test/harness'

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockReset()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

/** fetch accepts three input shapes. Reduce them all to a URL string. */
function requestUrl(input: RequestInfo | URL): string {
  if (typeof input === 'string') return input
  if (input instanceof URL) return input.href
  return input.url
}

/** The client always sends a JSON string body. Fail loudly if it does not. */
function requestBody(init: RequestInit | undefined): string {
  const body = init?.body
  if (typeof body !== 'string') {
    throw new Error(`expected a string request body, got ${typeof body}`)
  }
  return body
}

/** Pulls the single fetch call apart into the parts the contract fixes. */
function readSubmitCall() {
  expect(fetchMock).toHaveBeenCalledTimes(1)
  const call = fetchMock.mock.calls[0]
  if (call === undefined) throw new Error('fetch was not called')
  const [input, init] = call
  const url = new URL(requestUrl(input), 'http://console.test')
  const headers = new Headers(init?.headers)
  return { url, init, headers }
}

describe('JobForm', () => {
  it('POSTs /api/jobs with exactly the supplierId and payloadRef fields', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(
      jsonResponse({ jobId: 'a3f1c2d4-0000-4000-8000-000000000001', status: 'PENDING' }, 202),
    )

    renderWithQuery(<JobForm />)

    await user.type(screen.getByTestId('supplier-input'), 'acme-books')
    await user.type(screen.getByTestId('payload-input'), 's3://drop/batch-17.xml')
    await user.click(screen.getByTestId('submit-button'))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(1)
    })

    const { url, init, headers } = readSubmitCall()
    expect(url.pathname).toBe('/api/jobs')
    expect(init?.method).toBe('POST')
    expect(headers.get('Content-Type')).toBe('application/json')

    const body: unknown = JSON.parse(requestBody(init))
    expect(body).toEqual({
      supplierId: 'acme-books',
      payloadRef: 's3://drop/batch-17.xml',
    })
  })

  it('trims the inputs before it sends them', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(
      jsonResponse({ jobId: 'a3f1c2d4-0000-4000-8000-000000000002', status: 'PENDING' }, 202),
    )

    renderWithQuery(<JobForm />)

    await user.type(screen.getByTestId('supplier-input'), '  acme  ')
    await user.type(screen.getByTestId('payload-input'), '  s3://x  ')
    await user.click(screen.getByTestId('submit-button'))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(1)
    })

    const { init } = readSubmitCall()
    expect(JSON.parse(requestBody(init))).toEqual({
      supplierId: 'acme',
      payloadRef: 's3://x',
    })
  })

  it('keeps the submit button disabled until both fields have a value', async () => {
    const user = userEvent.setup()
    renderWithQuery(<JobForm />)

    const button = screen.getByTestId('submit-button')
    expect(button).toBeDisabled()

    await user.type(screen.getByTestId('supplier-input'), 'acme')
    expect(button).toBeDisabled()

    await user.type(screen.getByTestId('payload-input'), 's3://x')
    expect(button).toBeEnabled()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('shows the problem+json detail in an alert when the submit fails', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(problemResponse(400, 'supplierId is not known', 'Unknown supplier'))

    renderWithQuery(<JobForm />)

    await user.type(screen.getByTestId('supplier-input'), 'ghost')
    await user.type(screen.getByTestId('payload-input'), 's3://x')
    await user.click(screen.getByTestId('submit-button'))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('supplierId is not known')
  })
})
