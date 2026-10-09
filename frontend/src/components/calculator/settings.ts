export type LadderStep = { volume: number; margin: number }

export type CalculatorSettings = {
  euribor6m: number
  euriborDate: string
  defaultMargin: number
  marketAverageMargin: number
  worstCaseMargin: number
  switchingCost: number
  annualDiscountRate: number
  ladder: LadderStep[]
  committedVolume: number
}

export const defaultCalculatorSettings: CalculatorSettings = {
  euribor6m: 2.936,
  euriborDate: '2026-10-08',
  defaultMargin: 1.7,
  marketAverageMargin: 1.6,
  worstCaseMargin: 1.5,
  switchingCost: 400,
  annualDiscountRate: 3,
  ladder: [
    { volume: 25_000_000, margin: 1.45 },
    { volume: 50_000_000, margin: 1.35 },
    { volume: 100_000_000, margin: 1.25 },
  ],
  committedVolume: 0,
}

export type Scenario = 'worst' | 'expected' | 'best'

export const scenarios: Scenario[] = ['worst', 'expected', 'best']

export function scenarioMargins(settings: CalculatorSettings): Record<Scenario, number> {
  const ladder = [...settings.ladder].sort((a, b) => a.volume - b.volume)
  const nextStep = ladder.find((step) => step.volume > settings.committedVolume)
  const best = Math.min(...ladder.map((step) => step.margin))
  return {
    worst: settings.worstCaseMargin,
    expected: nextStep?.margin ?? best,
    best,
  }
}

export function useCalculatorSettings(): CalculatorSettings {
  return defaultCalculatorSettings
}
