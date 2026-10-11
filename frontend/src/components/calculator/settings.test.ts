import { describe, expect, it } from 'vitest'
import { publicConfig } from '../pool/fixtures'
import { defaultCalculatorSettings, minimumGoalMargin, toCalculatorSettings } from './settings'

describe('minimumGoalMargin', () => {
  it('takes the margin at the smallest milestone, whatever the order', () => {
    const [first, second, third] = defaultCalculatorSettings.ladder

    expect(minimumGoalMargin(defaultCalculatorSettings)).toBe(1.3)
    expect(
      minimumGoalMargin({ ...defaultCalculatorSettings, ladder: [third, first, second] }),
    ).toBe(1.3)
  })
})

describe('toCalculatorSettings', () => {
  it('takes the calculator values and the ladder from the public config', () => {
    const settings = toCalculatorSettings({
      ...publicConfig,
      calculator: { ...publicConfig.calculator, euribor6m: 3.1 },
    })

    expect(settings.euribor6m).toBe(3.1)
    expect(minimumGoalMargin(settings)).toBe(1.3)
  })

  it('keeps the default ladder when the config has fewer than three milestones', () => {
    const settings = toCalculatorSettings({
      ...publicConfig,
      pool: { ...publicConfig.pool, milestones: [] },
    })

    expect(settings.ladder).toEqual(defaultCalculatorSettings.ladder)
  })
})
