import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'

export type CreditCheck = {
  result: 'CLEAR' | 'DEFAULTS_FOUND'
  activeDefaults: number
  source: string
  checkedAt: string
}

const creditCheckKey = ['me', 'credit-check']

export function useCreditCheck() {
  return useQuery({
    queryKey: creditCheckKey,
    queryFn: () =>
      api.get<CreditCheck>('/v1/me/credit-check').catch((error: unknown) => {
        if (error instanceof ApiError && (error.status === 404 || error.status === 401)) {
          return null
        }
        throw error
      }),
  })
}

export function useRunCreditCheck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.post<CreditCheck>('/v1/me/credit-check', { consent: true }),
    onSuccess: (check) => queryClient.setQueryData(creditCheckKey, check),
  })
}
