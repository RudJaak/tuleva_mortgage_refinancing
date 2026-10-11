import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import type { LoanContractDocument, LoanView } from './api'
import { confirmedLoan, inProgressContract, processedContract } from './fixtures'
import { LoanStep } from './LoanStep'

const contractsPath = '/v1/documents?type=LOAN_CONTRACT'

function mockBackend({
  loan,
  contracts,
  status = 200,
}: {
  loan: LoanView | null
  contracts: LoanContractDocument[]
  status?: number
}) {
  const put = vi.fn()
  vi.mocked(fetch).mockImplementation((input, init) => {
    const path = String(input)
    if (status !== 200) {
      return Promise.resolve(new Response(null, { status }))
    }
    if (path === '/v1/me/loan' && init?.method === 'PUT') {
      put(JSON.parse(String(init.body)))
      return Promise.resolve(new Response(JSON.stringify(confirmedLoan), { status: 200 }))
    }
    if (path === '/v1/me/loan') {
      return Promise.resolve(
        loan
          ? new Response(JSON.stringify(loan), { status: 200 })
          : new Response(null, { status: 404 }),
      )
    }
    if (path === contractsPath) {
      return Promise.resolve(new Response(JSON.stringify(contracts), { status: 200 }))
    }
    return Promise.reject(new Error(`Unmocked ${path}`))
  })
  return put
}

function renderStep() {
  return renderWithProviders(
    <MemoryRouter>
      <LoanStep />
    </MemoryRouter>,
  )
}

describe('LoanStep', () => {
  it('shows who signed the contract and the progress while it is read', async () => {
    mockBackend({ loan: null, contracts: [inProgressContract] })
    renderStep()

    const card = await screen.findByTestId('contract-card')
    expect(within(card).getByTestId('seal-badge')).toHaveTextContent(
      'Digiallkirjastanud Kati Kask ja Mari Maasikas',
    )
    expect(within(card).getByRole('status')).toHaveTextContent('Loeme lepingut…')
    expect(within(card).getByRole('progressbar', { name: 'Lepingu lugemine' })).toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: 'Kontrolli lepingu andmeid' }),
    ).not.toBeInTheDocument()
  })

  it('shows only the upload before a contract has been read', async () => {
    mockBackend({ loan: null, contracts: [] })
    renderStep()

    expect(await screen.findByLabelText('Lisa laenuleping')).toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: 'Kontrolli lepingu andmeid' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Marginaal')).not.toBeInTheDocument()
  })

  it('pre-fills the fields with where each was read', async () => {
    mockBackend({ loan: null, contracts: [processedContract] })
    renderStep()

    expect(await screen.findByLabelText('Marginaal')).toHaveValue('2.95')
    expect(screen.getByLabelText('Pank')).toHaveValue('AS LHV Pank')
    expect(screen.getByLabelText('Tagastamise tähtpäev')).toHaveValue('2045-03-05')
    const evidence = screen.getAllByTestId('evidence')
    expect(evidence[0]).toHaveTextContent('Laenuleping, lk 1: „Euribor + 2,95% aastas“')
    expect(evidence[1]).toHaveTextContent('kontrolli üle')
    expect(screen.getByText(/kasutame lepingu põhjal hinnangut 66\s744\s€/)).toBeInTheDocument()
  })

  it('sends the confirmed and corrected values, then shows the savings', async () => {
    const put = mockBackend({ loan: null, contracts: [processedContract] })
    renderStep()

    const margin = await screen.findByLabelText('Marginaal')
    await userEvent.clear(margin)
    await userEvent.type(margin, '2,85')
    expect(screen.getByText('muudetud')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Praegune laenujääk'), '66 000')
    await userEvent.click(screen.getByRole('button', { name: 'Kinnitan andmed' }))

    expect(put).toHaveBeenCalledWith(
      expect.objectContaining({ documentId: 'c1', margin: 2.85, balance: 66000 }),
    )
    const confirmed = await screen.findByTestId('confirmed-loan')
    expect(within(confirmed).getByText('2 välja lepingust, 1 sinu sisestatud')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sinu võimalik sääst' })).toBeInTheDocument()
  })

  it('requires the key fields', async () => {
    const put = mockBackend({ loan: null, contracts: [processedContract] })
    renderStep()

    await userEvent.clear(await screen.findByLabelText('Marginaal'))
    await userEvent.click(screen.getByRole('button', { name: 'Kinnitan andmed' }))

    expect(await screen.findByText('Kohustuslik väli')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
  })

  it('shows the confirmed loan with its figures and lets the borrower edit it', async () => {
    mockBackend({ loan: confirmedLoan, contracts: [processedContract] })
    renderStep()

    const confirmed = await screen.findByTestId('confirmed-loan')
    expect(within(confirmed).getByText('5,886%')).toBeInTheDocument()
    expect(within(confirmed).getByText('sinu kinnitatud')).toBeInTheDocument()
    expect(within(confirmed).getByText('18 a 4 k')).toBeInTheDocument()
    expect(within(confirmed).getByText('41%')).toBeInTheDocument()
    expect(within(confirmed).getByText(/Kõik allolevad tingimused on täidetud/)).toBeInTheDocument()
    expect(within(confirmed).getAllByTestId('loan-check')[0]).toHaveTextContent(/Intress on ujuv/)
    expect(
      within(confirmed).getByText(/on kõrgem kui ühispakkumise esimene eesmärk 1,30%/),
    ).toBeInTheDocument()

    await userEvent.click(within(confirmed).getByRole('button', { name: 'Muuda andmeid' }))
    expect(await screen.findByLabelText('Praegune laenujääk')).toHaveValue('66000')
  })

  it('compares the confirmed margin with the minimum goal', async () => {
    mockBackend({ loan: confirmedLoan, contracts: [processedContract] })
    renderStep()

    await screen.findByTestId('confirmed-loan')
    expect(screen.getByTestId('goal-note')).toHaveTextContent(/miinimumeesmärgiga: marginaal 1,30%/)
    expect(screen.queryByRole('radio')).not.toBeInTheDocument()
  })

  it('offers a screenshot, the contract or manual entry side by side', async () => {
    mockBackend({ loan: null, contracts: [] })
    renderStep()

    expect(await screen.findByLabelText('Lisa ekraanipilt')).toBeInTheDocument()
    expect(screen.getByLabelText('Lisa laenuleping')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sisestan andmed ise' })).toBeInTheDocument()
  })

  it('says screenshots are not read yet', async () => {
    mockBackend({ loan: null, contracts: [] })
    renderStep()

    await userEvent.upload(
      await screen.findByLabelText('Lisa ekraanipilt'),
      new File(['png'], 'laen.png', { type: 'image/png' }),
    )

    expect(screen.getByText(/Ekraanipildi lugemine tuleb peagi/)).toBeInTheDocument()
  })

  it('lets the borrower enter the loan without a contract', async () => {
    mockBackend({ loan: null, contracts: [] })
    renderStep()

    await userEvent.click(await screen.findByRole('button', { name: 'Sisestan andmed ise' }))

    expect(screen.getByLabelText('Marginaal')).toHaveValue('')
    expect(screen.queryAllByTestId('evidence')).toHaveLength(0)
  })

  it('asks to log in first', async () => {
    mockBackend({ loan: null, contracts: [], status: 401 })
    renderStep()

    expect(await screen.findByRole('link', { name: 'logi sisse' })).toHaveAttribute(
      'href',
      '/sisene',
    )
  })
})
