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
  it('moves through the stages as time passes', () => {
    expect(stageFor(0)).toBe('statements.progress.reading')
    expect(stageFor(7_000)).toBe('statements.progress.income')
    expect(stageFor(14_000)).toBe('statements.progress.obligations')
    expect(stageFor(25_000)).toBe('statements.progress.calculating')
    expect(stageFor(60_000)).toBe('statements.progress.slow')
  })
})
