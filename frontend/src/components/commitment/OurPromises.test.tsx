import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../../test/render'
import { OurPromises } from './OurPromises'

describe('OurPromises', () => {
  it('lists what the pool does and what it does not', () => {
    renderWithProviders(<OurPromises />)

    const [weDo, weDont] = screen.getAllByRole('list')
    expect(within(weDo).getAllByRole('listitem')).toHaveLength(5)
    expect(within(weDo).getByText(/isikustamata/)).toBeInTheDocument()
    expect(within(weDont).getAllByRole('listitem')).toHaveLength(4)
    expect(within(weDont).getByText(/Me ei säilita sinu dokumente/)).toBeInTheDocument()
  })
})
