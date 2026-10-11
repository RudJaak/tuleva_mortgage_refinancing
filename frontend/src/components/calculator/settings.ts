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
    { members: 1000, margin: 1.3 },
    { members: 2500, margin: 1.25 },
    { members: 5000, margin: 1.2 },
  ],
}

/** The calculator uses the pool's minimum goal: the margin at the first (smallest) milestone. */
export function minimumGoalMargin(settings: CalculatorSettings): number {
  return [...settings.ladder].sort((a, b) => a.members - b.members)[0].margin
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
