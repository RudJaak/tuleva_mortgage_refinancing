import { usePublicConfig, type PublicConfig } from '../pool/api'

export type LadderStep = { members: number; margin: number }

export type CalculatorSettings = {
  euribor6m: number
  euriborDate: string
  defaultMargin: number
  marketAverageMargin: number
  switchingCost: number
  annualDiscountRate: number
  membershipFee: number
  ladder: [LadderStep, LadderStep, LadderStep]
}

export const defaultCalculatorSettings: CalculatorSettings = {
  euribor6m: 2.936,
  euriborDate: '2026-10-08',
  defaultMargin: 1.8,
  marketAverageMargin: 1.6,
  switchingCost: 400,
  annualDiscountRate: 3,
  membershipFee: 150,
  ladder: [
    { members: 1000, margin: 1.35 },
    { members: 2500, margin: 1.3 },
    { members: 5000, margin: 1.2 },
  ],
}

export type Scenario = 'worst' | 'expected' | 'best'

export const scenarios: Scenario[] = ['worst', 'expected', 'best']

export function scenarioMargins(settings: CalculatorSettings): Record<Scenario, number> {
  const [worst, expected, best] = [...settings.ladder]
    .sort((a, b) => a.members - b.members)
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
