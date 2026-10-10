import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'

export type CommitmentView = {
  committed: boolean
  committedBalance: number | null
  currentMargin: number | null
  signedAt: string | null
}

export type SigningMethod = 'SMART_ID' | 'MOBILE_ID'

export type SigningStart = {
  signingId: string
  verificationCode: string
  firstName: string | null
}

export type SigningStatus = 'PENDING' | 'VERIFYING' | 'COMPLETE' | 'FAILED'

export const commitmentKey = ['me', 'commitment']

export function useCommitment() {
  return useQuery({
    queryKey: commitmentKey,
    queryFn: () => api.get<CommitmentView>('/v1/me/commitment'),
  })
}

/** Signs with the login's method (and Mobile-ID phone), which the backend keeps in the session. */
export const startSigning = () => api.post<SigningStart>('/v1/me/commitment/signing', {})

export const pollSigning = (signingId: string) =>
  api.get<{ status: SigningStatus }>(`/v1/me/commitment/signing/${signingId}`)

export const cancelSigning = (signingId: string) =>
  api.delete<void>(`/v1/me/commitment/signing/${signingId}`)
