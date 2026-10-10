import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../../api/client'
import { cancelLogin, pollLogin, startLogin, type LoginMethod } from './api'

export const loginPollIntervalMs = 1000

export type LoginState =
  | { phase: 'idle'; errorId?: string }
  | {
      phase: 'pending'
      method: LoginMethod
      loginId: string
      verificationCode: string | null
      firstName: string | null
      /** Confirmed on the phone; the code is no longer needed while the login completes */
      verifying?: boolean
    }
  | { phase: 'success'; firstName: string | null }

export function useLogin() {
  const queryClient = useQueryClient()
  const [state, setState] = useState<LoginState>({ phase: 'idle' })

  const start = useCallback(
    async (method: LoginMethod, body?: { personalCode?: string; phoneNumber?: string }) => {
      setState({ phase: 'pending', method, loginId: '', verificationCode: null, firstName: null })
      try {
        const challenge = await startLogin(method, body)
        setState({ phase: 'pending', method, ...challenge })
      } catch (error) {
        const invalidInput = error instanceof ApiError && error.status === 400
        setState({
          phase: 'idle',
          errorId: !invalidInput
            ? 'login.error.generic'
            : method === 'smart-id'
              ? 'login.mobile.id.personal.code.invalid'
              : 'login.error.mobile.id.check.input',
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
        if (result.status === 'VERIFYING') {
          setState((current) =>
            current.phase === 'pending'
              ? { ...current, verificationCode: null, verifying: true }
              : current,
          )
          return
        }
        window.clearInterval(timer)
        if (result.status === 'COMPLETE') {
          setState({ phase: 'success', firstName: result.firstName })
          // A login can start the flow over on the server, so drop everything cached
          await queryClient.resetQueries()
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
