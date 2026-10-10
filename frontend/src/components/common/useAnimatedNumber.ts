import { useEffect, useRef, useState } from 'react'

const prefersReducedMotion = () =>
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const easeOutCubic = (t: number) => 1 - (1 - t) ** 3

/** Eases from the previously shown value to `target` whenever it changes; starts at `target`. */
export function useAnimatedNumber(target: number, durationMs = 1200): number {
  const [value, setValue] = useState(target)
  const shown = useRef(target)

  useEffect(() => {
    const from = shown.current
    const duration = prefersReducedMotion() ? 0 : durationMs
    let start: number | undefined
    let frame = 0
    const tick = (now: number) => {
      start ??= now
      const progress = duration === 0 ? 1 : Math.min(1, (now - start) / duration)
      shown.current = from + (target - from) * easeOutCubic(progress)
      setValue(shown.current)
      if (progress < 1) frame = requestAnimationFrame(tick)
    }
    if (from !== target) frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [target, durationMs])

  return value
}
