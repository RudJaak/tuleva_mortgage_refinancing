import { describe, expect, it } from 'vitest'
import { normalizeMobileIdPhoneNumber } from './mobileIdPhoneNumber'
import { isValidPersonalCode } from './personalCode'

describe('isValidPersonalCode', () => {
  it.each(['40504040001', '60001017869', '39901012239'])('accepts %s', (code) => {
    expect(isValidPersonalCode(code)).toBe(true)
  })

  it.each(['40504040002', '38888888888', '4050404000', '39913012236', 'abcdefghijk'])(
    'rejects %s',
    (code) => {
      expect(isValidPersonalCode(code)).toBe(false)
    },
  )
})

describe('normalizeMobileIdPhoneNumber', () => {
  it('adds the Estonian country code to a local number', () => {
    expect(normalizeMobileIdPhoneNumber('5123 4567')).toEqual({ phoneNumber: '+37251234567' })
  })

  it('rejects foreign numbers', () => {
    expect(normalizeMobileIdPhoneNumber('+358401234567')).toEqual({ problem: 'NOT_ESTONIAN' })
  })
})
