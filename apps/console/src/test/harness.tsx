import type { ReactElement, ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { RenderResult } from '@testing-library/react'

/** A client that never retries, so a failed request surfaces in one tick. */
export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0, refetchInterval: false },
      mutations: { retry: false },
    },
  })
}

export function renderWithQuery(ui: ReactElement): RenderResult {
  const queryClient = createTestQueryClient()
  function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
  return render(ui, { wrapper: Wrapper })
}

/** Builds a Response that matches what the control plane sends. */
export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/** Builds an RFC 9457 problem+json Response. */
export function problemResponse(
  status: number,
  detail: string,
  title = 'Bad Request',
): Response {
  return new Response(
    JSON.stringify({ type: 'about:blank', title, status, detail }),
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}
