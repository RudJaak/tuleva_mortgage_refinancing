import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { mockApi, renderWithProviders } from '../../test/render'
import { publicConfig } from '../pool/fixtures'
import { calculatorStorageKey, SavingsCalculator, type ValidatedLoan } from './SavingsCalculator'

function renderCalculator(validated?: ValidatedLoan) {
  return renderWithProviders(<SavingsCalculator validated={validated} />)
}

const monthlySaving = () => screen.getByTestId('monthly-saving').textContent
const lifetimeSaving = () => screen.getByTestId('lifetime-saving').textContent

describe('SavingsCalculator', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('shows a saving for the default loan at the goal margin', () => {
    renderCalculator()

    expect(screen.getByRole('radio', { name: /Eesmärk/ })).toBeChecked()
    expect(monthlySaving()).toMatch(/^\d+\s€ kuus$/)
    expect(lifetimeSaving()).toMatch(/laenu lõpuni$/)
  })

  it('recalculates when the balance changes', async () => {
    renderCalculator()
    const before = monthlySaving()

    const balance = screen.getByLabelText('Laenujääk')
    await userEvent.clear(balance)
    await userEvent.type(balance, '250000')

    expect(monthlySaving()).not.toBe(before)
  })

  it('counts the extra months on top of the years', async () => {
    renderCalculator()
    const before = Number(lifetimeSaving()!.replace(/\D/g, ''))

    await userEvent.type(screen.getByLabelText('Laenu lõpuni, lisaks kuud'), '{backspace}6')

    expect(Number(lifetimeSaving()!.replace(/\D/g, ''))).toBeGreaterThan(before)
  })

  it('caps the extra months at eleven', async () => {
    renderCalculator()
    const months = screen.getByLabelText('Laenu lõpuni, lisaks kuud')
    await userEvent.type(months, '{backspace}11')
    const elevenMonths = lifetimeSaving()

    await userEvent.clear(months)
    await userEvent.type(months, '40')

    expect(lifetimeSaving()).toBe(elevenMonths)
  })

  it('saves more in the best case than in the modest case', async () => {
    renderCalculator()

    await userEvent.click(screen.getByRole('radio', { name: /Tagasihoidlik/ }))
    const modest = Number(monthlySaving()!.replace(/\D/g, ''))
    await userEvent.click(screen.getByRole('radio', { name: /Parim/ }))
    const best = Number(monthlySaving()!.replace(/\D/g, ''))

    expect(best).toBeGreaterThan(modest)
  })

  it('falls back to a typical margin when the borrower does not know theirs', async () => {
    renderCalculator()

    await userEvent.click(screen.getByLabelText('Ei tea oma marginaali'))

    expect(screen.getByLabelText('Sinu praegune marginaal')).toBeDisabled()
    expect(screen.getByText(/Kasutame tüüpilist marginaali 1,70%/)).toBeInTheDocument()
  })

  it('explains when the scenario would not save anything', async () => {
    window.sessionStorage.setItem(calculatorStorageKey, JSON.stringify({ margin: 0.9 }))
    renderCalculator()

    expect(screen.getByText(/juba sama madal või madalam/)).toBeInTheDocument()
  })

  it('remembers the inputs for the rest of the session', async () => {
    const { unmount } = renderCalculator()
    const balance = screen.getByLabelText('Laenujääk')
    await userEvent.clear(balance)
    await userEvent.type(balance, '180000')
    unmount()

    renderCalculator()

    expect(screen.getByLabelText('Laenujääk')).toHaveValue(180000)
  })

  it('shows validated loan figures without editable inputs', () => {
    renderCalculator({ balance: 98_500, remainingMonths: 222, margin: 1.95 })

    expect(screen.getByText('18 a 6 k')).toBeInTheDocument()

    expect(screen.queryByLabelText('Laenujääk')).not.toBeInTheDocument()
    expect(screen.getAllByText('✓ lepingust')).toHaveLength(3)
    expect(screen.getByText(/98\s500\s€/)).toBeInTheDocument()
    expect(screen.getByText('1,95%')).toBeInTheDocument()
  })

  it('uses the Euribor and tiers served by the backend', async () => {
    mockApi({
      '/v1/config/public': {
        ...publicConfig,
        calculator: { ...publicConfig.calculator, euribor6m: 3.25 },
      },
    })
    renderCalculator()

    expect(await screen.findByText(/6 kuu Euribor 3,25%/, { selector: 'p' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Eesmärk/ })).toHaveAccessibleName(/1,30%/)
  })

  it('presents margins as goals with a disclaimer, not as an offer', () => {
    renderCalculator()

    expect(screen.getByText(/mitte pakkumine/)).toBeInTheDocument()
  })
})
