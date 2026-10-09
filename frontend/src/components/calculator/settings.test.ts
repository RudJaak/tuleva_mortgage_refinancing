import { describe, expect, it } from 'vitest'
import { defaultCalculatorSettings, scenarioMargins } from './settings'

describe('scenarioMargins', () => {
  it('maps the three ladder tiers to the modest, goal and best scenarios', () => {
    expect(scenarioMargins(defaultCalculatorSettings)).toEqual({
      worst: 1.3,
      expected: 1.2,
      best: 1.1,
    })
  })

  it('orders tiers by pool volume regardless of input order', () => {
    const [first, second, third] = defaultCalculatorSettings.ladder

    expect(
      scenarioMargins({ ...defaultCalculatorSettings, ladder: [third, first, second] }),
    ).toEqual({ worst: 1.3, expected: 1.2, best: 1.1 })
  })
})
