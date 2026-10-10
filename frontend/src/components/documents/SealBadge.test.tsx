import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../../test/render'
import type { Verification } from './api'
import { SealBadge } from './SealBadge'

const signed: Verification = {
  level: 'SIGNED',
  bankName: null,
  sealedBy: null,
  sealedAt: '2020-03-05T10:00:00Z',
  signers: [
    { name: 'Kati Kask', kind: 'PERSON' },
    { name: 'Mari Maasikas', kind: 'PERSON' },
  ],
  signedByBorrower: true,
}

describe('SealBadge', () => {
  it.each([
    [signed, 'Digiallkirjastanud Kati Kask ja Mari Maasikas'],
    [
      { ...signed, signedByBorrower: false },
      'Allkirjastanud Kati Kask ja Mari Maasikas, sinu allkirja pole',
    ],
    [{ ...signed, signers: [] }, 'Digiallkirjastatud'],
    [{ ...signed, signers: [], signedByBorrower: false }, 'Digiallkirjastatud, sinu allkirja pole'],
    [
      { ...signed, level: 'BANK_SEALED' as const, bankName: 'LHV', signers: [] },
      'LHV digitempel kinnitatud',
    ],
  ])('describes %#', (verification, text) => {
    renderWithProviders(<SealBadge verification={verification} />)

    expect(screen.getByTestId('seal-badge')).toHaveTextContent(text)
  })
})
