import { describe, expect, it } from 'vitest'
import { remainingUntil } from './remaining'

describe('remainingUntil', () => {
  it('splits the time left into days, hours, minutes and seconds', () => {
    expect(
      remainingUntil(new Date('2026-12-31T23:59:59+02:00'), new Date('2026-10-10T12:30:00+03:00')),
    ).toEqual({ days: 82, hours: 12, minutes: 29, seconds: 59 })
  })

  it('stops at zero after the deadline', () => {
    expect(
      remainingUntil(new Date('2026-12-31T23:59:59Z'), new Date('2027-01-02T00:00:00Z')),
    ).toEqual({ days: 0, hours: 0, minutes: 0, seconds: 0 })
  })
})
