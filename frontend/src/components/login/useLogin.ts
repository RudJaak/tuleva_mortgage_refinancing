import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../../api/client'
import { cancelLogin, meQueryKey, pollLogin, startLogin, type LoginMethod } from './api'

export const loginPollIntervalMs = 1000

export type LoginState =
  | { phase: 'idle'; errorId?: string }
  | {
      phase: 'pending'
      method: LoginMethod
      loginId: string
      verificationCode: string | null
      deviceLink: string | null
    }
  | { phase: 'success'; firstName: string | null }

export function useLogin() {
  const queryClient = useQueryClient()
  const [state, setState] = useState<LoginState>({ phase: 'idle' })

  const start = useCallback(
    async (method: LoginMethod, body?: { personalCode?: string; phoneNumber?: string }) => {
      setState({ phase: 'pending', method, loginId: '', verificationCode: null, deviceLink: null })
      try {
        const challenge = await startLogin(method, body)
        setState({ phase: 'pending', method, ...challenge })
      } catch (error) {
        const invalidInput = error instanceof ApiError && error.status === 400
        setState({
          phase: 'idle',
          errorId: invalidInput ? 'login.error.mobile.id.check.input' : 'login.error.generic',
        })
      }
    },
    [],
  )

  const loginId = state.phase === 'pending' ? state.loginId : ''

  useEffect(() => {
    if (!loginId) {
      return
    }
    let stopped = false
    const timer = window.setInterval(async () => {
      try {
        const result = await pollLogin(loginId)
        if (stopped || result.status === 'PENDING') {
          return
        }
        window.clearInterval(timer)
        if (result.status === 'COMPLETE') {
          setState({ phase: 'success', firstName: result.firstName })
          await queryClient.invalidateQueries({ queryKey: meQueryKey })
        } else {
          setState({ phase: 'idle', errorId: 'login.error.generic' })
        }
      } catch {
        window.clearInterval(timer)
        if (!stopped) {
          setState({ phase: 'idle', errorId: 'login.error.generic' })
        }
      }
    }, loginPollIntervalMs)
    return () => {
      stopped = true
      window.clearInterval(timer)
    }
  }, [loginId, queryClient])

  const cancel = useCallback(() => {
    if (loginId) {
      cancelLogin(loginId).catch(() => undefined)
    }
    setState({ phase: 'idle' })
  }, [loginId])

  return { state, start, cancel }
}
