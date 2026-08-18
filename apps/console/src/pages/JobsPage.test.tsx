import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { JobsPage } from './JobsPage'
import { JOB_POLL_INTERVAL_MS, jobKeys } from '../api/queries'
import { jsonResponse } from '../test/harness'

const fetchMock = vi.fn<typeof fetch>()
let hidden = false

beforeEach(() => {
  hidden = false
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => (hidden ? 'hidden' : 'visible'),
  })
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockReset()
  fetchMock.mockResolvedValue(jsonResponse({ items: [], nextCursor: null }))
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  return {
    client,
    ...render(
      <QueryClientProvider client={client}>
        <JobsPage />
      </QueryClientProvider>,
    ),
  }
}

describe('JobsPage', () => {
  it('exposes every data-testid the Playwright test depends on', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        items: [
          {
            id: 'a3f1c2d4-0000-4000-8000-000000000001',
            supplierId: 'acme',
            payloadRef: 's3://x',
            status: 'PENDING',
            workerId: null,
            result: null,
            createdAt: '2026-08-13T09:00:00Z',
            updatedAt: '2026-08-13T09:00:00Z',
          },
        ],
        nextCursor: null,
      }),
    )
    renderPage()

    await screen.findByTestId('job-row')

    for (const id of [
      'submit-form',
      'supplier-input',
      'payload-input',
      'submit-button',
      'job-row',
      'job-status',
    ]) {
      expect(document.querySelector(`[data-testid="${id}"]`), id).not.toBeNull()
    }

    expect(document.querySelector('[data-testid="submit-form"]')?.tagName).toBe('FORM')
    expect(document.querySelector('[data-testid="supplier-input"]')?.tagName).toBe('INPUT')
    expect(document.querySelector('[data-testid="payload-input"]')?.tagName).toBe('INPUT')
    expect(document.querySelector('[data-testid="job-row"]')?.tagName).toBe('TR')
    // job-status must sit inside job-row so a Playwright row scope finds it
    expect(
      document
        .querySelector('[data-testid="job-row"]')
        ?.querySelector('[data-testid="job-status"]'),
    ).not.toBeNull()
  })

  it('polls about once a second and stops while the tab is hidden', async () => {
    vi.useFakeTimers()
    renderPage()

    // let the initial fetch settle without waitFor, which deadlocks on fake timers
    await vi.advanceTimersByTimeAsync(20)
    const afterFirst = fetchMock.mock.calls.length
    expect(afterFirst).toBeGreaterThanOrEqual(1)

    await vi.advanceTimersByTimeAsync(3000)
    const afterPolling = fetchMock.mock.calls.length
    expect(afterPolling).toBeGreaterThan(afterFirst)

    // hide the tab
    hidden = true
    document.dispatchEvent(new Event('visibilitychange'))
    await vi.advanceTimersByTimeAsync(50)
    const atHide = fetchMock.mock.calls.length

    await vi.advanceTimersByTimeAsync(5000)
    expect(fetchMock.mock.calls.length).toBe(atHide)
  })

  /**
   * The test above asserts a behaviour TanStack Query owns. It cannot fail while the
   * option that buys that behaviour is missing, because the library's own default is
   * the same as the value we pass: query-core skips a scheduled refetch unless
   * `refetchIntervalInBackground` is truthy or the tab is focused. Dropping the option
   * from `useJobsQuery` therefore changes nothing that a rendered page can show.
   *
   * That is exactly why the option must be asserted on its own. This reads the resolved
   * options of the live observer, so it fails the moment `queries.ts` stops stating the
   * choice, and it keeps failing if someone later sets it to `true`.
   */
  it('states refetchIntervalInBackground: false on the jobs query', async () => {
    const { client } = renderPage()

    await screen.findByTestId('submit-form')

    const query = client.getQueryCache().find({ queryKey: jobKeys.list(50) })
    expect(query, 'the jobs query must be in the cache').toBeDefined()

    const options = query?.observers[0]?.options
    expect(options?.refetchInterval).toBe(JOB_POLL_INTERVAL_MS)
    expect(options?.refetchIntervalInBackground).toBe(false)
  })
})
