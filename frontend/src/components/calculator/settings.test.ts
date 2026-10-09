import { describe, expect, it } from 'vitest'
import { defaultCalculatorSettings, scenarioMargins } from './settings'

describe('scenarioMargins', () => {
  it('aims for the next ladder step that the pool has not reached', () => {
    const margins = scenarioMargins({ ...defaultCalculatorSettings, committedVolume: 32_000_000 })

    expect(margins).toEqual({ worst: 1.5, expected: 1.35, best: 1.25 })
  })

  it('aims for the first step of an empty pool', () => {
    expect(scenarioMargins(defaultCalculatorSettings).expected).toBe(1.45)
  })

  it('uses the best margin once every step is reached', () => {
    const margins = scenarioMargins({ ...defaultCalculatorSettings, committedVolume: 150_000_000 })

    expect(margins.expected).toBe(1.25)
  })
})
