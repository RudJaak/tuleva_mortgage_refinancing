import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAnimatedNumber } from './useAnimatedNumber'

describe('useAnimatedNumber', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('starts at the target without animating', () => {
    const { result } = renderHook(() => useAnimatedNumber(520))

    expect(result.current).toBe(520)
  })

  it('eases towards a new target and lands on it exactly', () => {
    const { result, rerender } = renderHook(({ target }) => useAnimatedNumber(target, 1000), {
      initialProps: { target: 520 },
    })

    rerender({ target: 530 })
    act(() => {
      vi.advanceTimersByTime(300)
    })
    expect(result.current).toBeGreaterThan(520)
    expect(result.current).toBeLessThan(530)

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(result.current).toBe(530)
  })
})
