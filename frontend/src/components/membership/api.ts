import { useQuery } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'

/** Joining the pool requires Tuleva membership (D32); in the demo everyone is a member. */
export function useMembership() {
  return useQuery({
    queryKey: ['me', 'membership'],
    queryFn: () =>
      api.get<{ member: boolean }>('/v1/me/membership').catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 401) {
          return null
        }
        throw error
      }),
    staleTime: Infinity,
  })
}
