import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { IntlProvider } from 'react-intl'
import { locale, messages } from '../translations'

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } }),
  )
  return (
    <IntlProvider locale={locale} messages={messages} defaultLocale={locale}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </IntlProvider>
  )
}
