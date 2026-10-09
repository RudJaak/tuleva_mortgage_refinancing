import { describe, expect, it } from 'vitest'
import { annuityPayment, compareMargins, totalInterest } from './savings'

const reference = {
  balance: 125_000,
  remainingMonths: 240,
  euribor: 2.5,
  currentMargin: 1.85,
  switchingCost: 400,
  annualDiscountRate: 3,
}

describe('annuityPayment', () => {
  it('matches the reference loan', () => {
    expect(annuityPayment(125_000, 4.35, 240)).toBeCloseTo(780.73, 2)
  })

  it('splits the principal evenly at a zero rate', () => {
    expect(annuityPayment(120_000, 0, 240)).toBe(500)
  })

  it('is zero when nothing is owed or no months remain', () => {
    expect(annuityPayment(0, 4, 240)).toBe(0)
    expect(annuityPayment(100_000, 4, 0)).toBe(0)
  })
})

describe('totalInterest', () => {
  it('is all payments minus the principal', () => {
    expect(totalInterest(125_000, 4.35, 240)).toBeCloseTo(
      annuityPayment(125_000, 4.35, 240) * 240 - 125_000,
      6,
    )
    expect(totalInterest(125_000, 4.35, 240)).toBeCloseTo(62_374.37, 2)
  })
})

describe('compareMargins', () => {
  it('reproduces the 0.5 pp reference scenario', () => {
    const result = compareMargins({ ...reference, targetMargin: 1.35 })

    expect(result.currentPayment).toBeCloseTo(780.73, 2)
    expect(result.targetPayment).toBeCloseTo(747.63, 2)
    expect(result.monthlySaving).toBeCloseTo(33.09, 2)
    expect(result.lifetimeSaving).toBeCloseTo(7942.69, 2)
    expect(result.netPresentValue).toBeCloseTo(5567.31, 2)
    expect(result.breakEvenMonths).toBeCloseTo(12.1, 1)
  })

  it('reports no saving when the target margin is not lower', () => {
    const result = compareMargins({ ...reference, targetMargin: 1.85 })

    expect(result.monthlySaving).toBe(0)
    expect(result.lifetimeSaving).toBe(0)
    expect(result.netPresentValue).toBe(0)
    expect(result.breakEvenMonths).toBeNull()
    expect(result.hasSaving).toBe(false)
  })

  it('grows with a larger margin cut', () => {
    const half = compareMargins({ ...reference, targetMargin: 1.35 })
    const full = compareMargins({ ...reference, targetMargin: 0.85 })

    expect(full.monthlySaving).toBeGreaterThan(half.monthlySaving)
    expect(full.netPresentValue).toBeGreaterThan(half.netPresentValue)
  })

  it('handles an empty loan without dividing by zero', () => {
    const result = compareMargins({ ...reference, balance: 0, targetMargin: 1.35 })

    expect(result.monthlySaving).toBe(0)
    expect(result.breakEvenMonths).toBeNull()
  })
})
