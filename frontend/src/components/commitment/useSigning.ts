import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../../api/client'
import { cancelSigning, pollSigning, startSigning } from './api'

export const signingPollIntervalMs = 1000

export type SigningState =
  | { phase: 'idle'; errorReason?: string }
  | { phase: 'pending'; signingId: string; verificationCode: string | null; verifying: boolean }
  | { phase: 'signed' }

/** The emulated Smart-ID / Mobile-ID signature, polled like the login (D29, T41). */
export function useSigning() {
  const queryClient = useQueryClient()
  const [state, setState] = useState<SigningState>({ phase: 'idle' })

  const sign = useCallback(async () => {
    setState({ phase: 'pending', signingId: '', verificationCode: null, verifying: false })
    try {
      const start = await startSigning()
      setState({
        phase: 'pending',
        signingId: start.signingId,
        verificationCode: start.verificationCode,
        verifying: false,
      })
    } catch (error) {
      setState({
        phase: 'idle',
        errorReason: error instanceof ApiError ? (error.reason ?? 'GENERIC') : 'GENERIC',
      })
    }
  }, [])

  const signingId = state.phase === 'pending' ? state.signingId : ''

  useEffect(() => {
    if (!signingId) {
      return
    }
    let stopped = false
    const timer = window.setInterval(async () => {
      try {
        const { status } = await pollSigning(signingId)
        if (stopped || status === 'PENDING') {
          return
        }
        if (status === 'VERIFYING') {
          setState((current) =>
            current.phase === 'pending'
              ? { ...current, verificationCode: null, verifying: true }
              : current,
          )
          return
        }
        window.clearInterval(timer)
        if (status === 'COMPLETE') {
          await queryClient.invalidateQueries()
          setState({ phase: 'signed' })
        } else {
          setState({ phase: 'idle', errorReason: 'GENERIC' })
        }
      } catch {
        window.clearInterval(timer)
        if (!stopped) {
          setState({ phase: 'idle', errorReason: 'GENERIC' })
        }
      }
    }, signingPollIntervalMs)
    return () => {
      stopped = true
      window.clearInterval(timer)
    }
  }, [signingId, queryClient])

  const cancel = useCallback(() => {
    if (signingId) {
      cancelSigning(signingId).catch(() => undefined)
    }
    setState({ phase: 'idle' })
  }, [signingId])

  return { state, sign, cancel }
}
