import { screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../../test/render'
import { LandingPage } from './LandingPage'

describe('LandingPage', () => {
  it('shows the counter first, then the calculator, and the join button last', () => {
    renderWithProviders(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    )

    const tracker = screen.getByRole('heading', { name: 'Ühispakkumise seis' })
    const calculator = screen.getByRole('radio', { name: /Eesmärk/ })
    const join = screen.getByRole('link', { name: 'Liitu ühispakkumisega' })
    const follows = (a: Element, b: Element) =>
      Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING)
    expect(follows(tracker, calculator)).toBe(true)
    expect(follows(calculator, join)).toBe(true)
    expect(screen.getAllByRole('link', { name: 'Liitu ühispakkumisega' })).toHaveLength(1)
  })
})
