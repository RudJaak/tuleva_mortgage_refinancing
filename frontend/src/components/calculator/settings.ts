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
    { volume: 25_000_000, margin: 1.3 },
    { volume: 50_000_000, margin: 1.2 },
    { volume: 100_000_000, margin: 1.1 },
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

export function useCalculatorSettings(): CalculatorSettings {
  return defaultCalculatorSettings
}
