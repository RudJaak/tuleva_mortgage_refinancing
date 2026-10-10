import { QueryClient } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { vi } from 'vitest'
import { AppProviders } from '../components/AppProviders'

export function renderWithProviders(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<AppProviders queryClient={queryClient}>{ui}</AppProviders>)
}

export function mockApi(responses: Record<string, unknown>) {
  vi.mocked(fetch).mockImplementation((input) => {
    const path = String(input)
    if (!(path in responses)) {
      return Promise.reject(new Error(`Unmocked ${path}`))
    }
    return Promise.resolve(new Response(JSON.stringify(responses[path]), { status: 200 }))
  })
}
