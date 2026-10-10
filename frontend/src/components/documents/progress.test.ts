import { describe, expect, it } from 'vitest'
import { estimatedProgress, stageFor } from './progress'

describe('estimatedProgress', () => {
  it('starts at zero and is well along by the typical duration', () => {
    expect(estimatedProgress(0)).toBe(0)
    expect(estimatedProgress(30_000)).toBeGreaterThanOrEqual(80)
  })

  it('never claims to be done while still waiting', () => {
    expect(estimatedProgress(10 * 60_000)).toBe(95)
  })

  it('treats a server clock slightly ahead as no time elapsed', () => {
    expect(estimatedProgress(-2_000)).toBe(0)
  })
})

describe('stageFor', () => {
  const stages = [
    { fromSeconds: 0, id: 'first' },
    { fromSeconds: 6, id: 'second' },
    { fromSeconds: 45, id: 'slow' },
  ]

  it('moves through the stages as time passes', () => {
    expect(stageFor(0, stages)).toBe('first')
    expect(stageFor(7_000, stages)).toBe('second')
    expect(stageFor(60_000, stages)).toBe('slow')
  })
})
