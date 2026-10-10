import { useQuery } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'

export type LoginMethod = 'smart-id' | 'mobile-id' | 'id-card'

export type LoginStart = {
  loginId: string
  verificationCode: string | null
  deviceLink: string | null
}

export type LoginPoll = {
  status: 'PENDING' | 'COMPLETE' | 'FAILED'
  firstName: string | null
}

export type Me = { firstName: string | null; lastName: string | null }

export const meQueryKey = ['me']

export const startLogin = (
  method: LoginMethod,
  body: { personalCode?: string; phoneNumber?: string } = {},
) => api.post<LoginStart>(`/v1/auth/${method}`, body)

export const pollLogin = (loginId: string) => api.get<LoginPoll>(`/v1/auth/logins/${loginId}`)

export const cancelLogin = (loginId: string) => api.delete<void>(`/v1/auth/logins/${loginId}`)

export const logout = () => api.post<void>('/v1/auth/logout', {})

export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: async () => {
      try {
        return await api.get<Me>('/v1/me')
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          return null
        }
        throw error
      }
    },
  })
}
