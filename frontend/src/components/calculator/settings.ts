import { usePublicConfig, type PublicConfig } from '../pool/api'

export type LadderStep = { volume: number; margin: number }

export type CalculatorSettings = {
  euribor6m: number
  euriborDate: string
  defaultMargin: number
  marketAverageMargin: number
  switchingCost: number
  annualDiscountRate: number
  ladder: [LadderStep, LadderStep, LadderStep]
}

export const defaultCalculatorSettings: CalculatorSettings = {
  euribor6m: 2.936,
  euriborDate: '2026-10-08',
  defaultMargin: 1.7,
  marketAverageMargin: 1.6,
  switchingCost: 400,
  annualDiscountRate: 3,
  ladder: [
    { volume: 100_000_000, margin: 1.2 },
    { volume: 300_000_000, margin: 1.0 },
    { volume: 500_000_000, margin: 0.8 },
  ],
}

export type Scenario = 'worst' | 'expected' | 'best'

export const scenarios: Scenario[] = ['worst', 'expected', 'best']

export function scenarioMargins(settings: CalculatorSettings): Record<Scenario, number> {
  const [worst, expected, best] = [...settings.ladder]
    .sort((a, b) => a.volume - b.volume)
    .map((step) => step.margin)
  return { worst, expected, best }
}

export function toCalculatorSettings(config: PublicConfig): CalculatorSettings {
  const [first, second, third] = config.pool.milestones
  if (!first || !second || !third) {
    return { ...defaultCalculatorSettings, ...config.calculator }
  }
  return { ...config.calculator, ladder: [first, second, third] }
}

export function useCalculatorSettings(): CalculatorSettings {
  const { data } = usePublicConfig()
  return data ? toCalculatorSettings(data) : defaultCalculatorSettings
}
