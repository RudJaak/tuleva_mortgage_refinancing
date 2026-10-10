import { describe, expect, it } from 'vitest'
import { publicConfig } from '../pool/fixtures'
import { defaultCalculatorSettings, scenarioMargins, toCalculatorSettings } from './settings'

describe('scenarioMargins', () => {
  it('maps the three ladder tiers to the modest, goal and best scenarios', () => {
    expect(scenarioMargins(defaultCalculatorSettings)).toEqual({
      worst: 1.35,
      expected: 1.3,
      best: 1.2,
    })
  })

  it('orders tiers by the number of people regardless of input order', () => {
    const [first, second, third] = defaultCalculatorSettings.ladder

    expect(
      scenarioMargins({ ...defaultCalculatorSettings, ladder: [third, first, second] }),
    ).toEqual({ worst: 1.35, expected: 1.3, best: 1.2 })
  })
})

describe('toCalculatorSettings', () => {
  it('takes the calculator values and the ladder from the public config', () => {
    const settings = toCalculatorSettings({
      ...publicConfig,
      calculator: { ...publicConfig.calculator, euribor6m: 3.1 },
    })

    expect(settings.euribor6m).toBe(3.1)
    expect(scenarioMargins(settings)).toEqual({ worst: 1.35, expected: 1.3, best: 1.2 })
  })

  it('keeps the default ladder when the config has fewer than three milestones', () => {
    const settings = toCalculatorSettings({
      ...publicConfig,
      pool: { ...publicConfig.pool, milestones: [] },
    })

    expect(settings.ladder).toEqual(defaultCalculatorSettings.ladder)
  })
})
