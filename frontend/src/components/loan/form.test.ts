import { describe, expect, it } from 'vitest'
import { parseNumber, toFormValues, toTerms } from './form'

describe('parseNumber', () => {
  it('reads Estonian and plain number formats', () => {
    expect(parseNumber('1,95')).toBe(1.95)
    expect(parseNumber('185 000')).toBe(185000)
    expect(parseNumber('920.00 €')).toBe(920)
    expect(parseNumber('')).toBeNull()
    expect(parseNumber('abc')).toBeNull()
  })
})

describe('toTerms', () => {
  it('turns the form back into terms, dropping a fixed-rate end for a variable loan', () => {
    const values = {
      ...toFormValues(null),
      lender: ' LHV ',
      margin: '2,95',
      fixedRateUntil: '2027-01-01',
      maturityDate: '2045-03-05',
    }

    expect(toTerms(values, 'd1')).toMatchObject({
      documentId: 'd1',
      lender: 'LHV',
      margin: 2.95,
      rateType: 'VARIABLE',
      fixedRateUntil: null,
      maturityDate: '2045-03-05',
      balance: null,
    })
  })
})
