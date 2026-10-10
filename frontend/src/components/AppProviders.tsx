import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { IntlProvider } from 'react-intl'
import { locale, messages } from '../translations'

function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } })
}

export function AppProviders({
  children,
  queryClient,
}: {
  children: ReactNode
  queryClient?: QueryClient
}) {
  const [defaultClient] = useState(createQueryClient)
  return (
    <IntlProvider locale={locale} messages={messages} defaultLocale={locale}>
      <QueryClientProvider client={queryClient ?? defaultClient}>{children}</QueryClientProvider>
    </IntlProvider>
  )
}
