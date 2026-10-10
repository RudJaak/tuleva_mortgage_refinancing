import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import type { StatementDocument } from './api'
import { inProgress, processed } from './fixtures'
import { StatementsStep } from './StatementsStep'

const listPath = '/v1/documents?type=ACCOUNT_STATEMENT'

type Overview = { persons: unknown[]; household: unknown }

const totals = {
  monthlyIncome: 2480,
  monthlyObligations: 1001.4,
  monthlyCardRepayments: 1200,
  monthlySavings: 100,
  debtServiceRatio: 0.4,
}

function overviewOf(list: StatementDocument[] | { status: number }): Overview {
  const ids = Array.isArray(list)
    ? list.filter((d) => d.status === 'PROCESSED').map((d) => d.documentId)
    : []
  return {
    persons: [{ name: 'Mari Maasikas', onLoan: true, documentIds: ids, totals, flags: [] }],
    household: totals,
  }
}

function mockDocuments(
  list: StatementDocument[] | { status: number },
  upload?: { status: number; body: unknown },
  overview: Overview = overviewOf(list),
) {
  vi.mocked(fetch).mockImplementation((input, init) => {
    const path = String(input)
    if (path === '/v1/me/statements') {
      return Promise.resolve(new Response(JSON.stringify(overview), { status: 200 }))
    }
    if (path === '/v1/documents' && init?.method === 'POST' && upload) {
      return Promise.resolve(new Response(JSON.stringify(upload.body), { status: upload.status }))
    }
    if (path === listPath) {
      return Promise.resolve(
        Array.isArray(list)
          ? new Response(JSON.stringify(list), { status: 200 })
          : new Response(null, { status: list.status }),
      )
    }
    return Promise.reject(new Error(`Unmocked ${path}`))
  })
}

describe('StatementsStep', () => {
  it('shows the verified seal and an estimated progress while the statement is read', async () => {
    mockDocuments([inProgress])
    renderStep()

    const card = await screen.findByTestId('statement-card')
    expect(within(card).getByTestId('seal-badge')).toHaveTextContent('LHV digitempel kinnitatud')
    expect(
      within(card).getByRole('progressbar', { name: 'Väljavõtte analüüs' }),
    ).toBeInTheDocument()
    expect(within(card).getByRole('status')).toHaveTextContent('Loeme väljavõtet…')
    expect(within(card).getByText(/Võid vahepeal jätkata järgmise sammuga/)).toBeInTheDocument()
  })

  it('shows the summary once the statement is processed', async () => {
    mockDocuments([processed])
    renderStep()

    const card = await screen.findByTestId('statement-card')
    expect(within(card).getByRole('heading', { name: 'Konto EE…3456' })).toBeInTheDocument()
    expect(within(card).getByText('2480 €/kuus')).toBeInTheDocument()
    expect(within(card).getByText('1001 €/kuus')).toBeInTheDocument()
    expect(within(card).getByText('40% sissetulekust')).toBeInTheDocument()
    expect(within(card).getByText('1200 €/kuus')).toBeInTheDocument()
    expect(within(card).getByText(/Autoliising AS: 3500 €/)).toBeInTheDocument()
    expect(within(card).getByText('Konto omanik on sina')).toBeInTheDocument()
    expect(within(card).getByTestId('statement-flag')).toHaveTextContent('Viivised: 1')
    expect(within(card).queryByRole('progressbar')).not.toBeInTheDocument()
  })

  it('adds an uploaded statement straight away with its seal', async () => {
    mockDocuments([], { status: 202, body: inProgress })
    renderStep()

    await userEvent.upload(
      await screen.findByLabelText('Lisa väljavõtted'),
      new File(['%PDF'], 'statement.asice', { type: 'application/vnd.etsi.asic-e+zip' }),
    )

    const card = await screen.findByTestId('statement-card')
    expect(within(card).getByTestId('seal-badge')).toHaveTextContent('LHV digitempel kinnitatud')
    expect(within(card).getByRole('progressbar')).toBeInTheDocument()
  })

  it('explains a rejected upload', async () => {
    mockDocuments([], { status: 422, body: { reason: 'SEAL_REQUIRED' } })
    renderStep()

    await userEvent.upload(
      await screen.findByLabelText('Lisa väljavõtted'),
      new File(['%PDF'], 'statement.pdf', { type: 'application/pdf' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Lisa panga digitempliga väljavõte (.asice).',
    )
  })

  it('asks every borrower on the loan for their statements and adds them up', async () => {
    const second = { ...processed, documentId: 'd2' }
    mockDocuments([processed, second], undefined, {
      persons: [
        { name: 'Jaan Tamm', onLoan: true, documentIds: ['d1', 'd2'], totals, flags: [] },
        { name: 'Mari Tamm', onLoan: true, documentIds: [], totals, flags: [] },
      ],
      household: { ...totals, monthlyIncome: 4960 },
    })
    renderStep()

    expect(await screen.findByTestId('co-borrowers')).toHaveTextContent(
      'Laenulepingus on 2 laenusaajat: Jaan Tamm ja Mari Tamm.',
    )
    const [jaan, mari] = screen.getAllByTestId('statement-person')
    expect(within(jaan).getByText(/2 konto väljavõtted/)).toBeInTheDocument()
    expect(within(jaan).getAllByTestId('statement-card')).toHaveLength(2)
    expect(within(mari).getByText('väljavõtted puuduvad')).toBeInTheDocument()
    expect(screen.queryByTestId('household-totals')).not.toBeInTheDocument()
  })

  it('uploads several statements at once', async () => {
    mockDocuments([], { status: 202, body: inProgress })
    renderStep()

    await userEvent.upload(await screen.findByLabelText('Lisa väljavõtted'), [
      new File(['%PDF'], 'lhv.asice', { type: 'application/vnd.etsi.asic-e+zip' }),
      new File(['%PDF'], 'swedbank.asice', { type: 'application/vnd.etsi.asic-e+zip' }),
    ])

    const posts = vi
      .mocked(fetch)
      .mock.calls.filter(
        ([path, init]) => String(path) === '/v1/documents' && init?.method === 'POST',
      )
    expect(posts).toHaveLength(2)
  })

  it('asks to log in first', async () => {
    mockDocuments({ status: 401 })
    renderStep()

    expect(await screen.findByRole('link', { name: 'logi sisse' })).toHaveAttribute(
      'href',
      '/sisene',
    )
  })
})

function renderStep() {
  return renderWithProviders(
    <MemoryRouter>
      <StatementsStep />
    </MemoryRouter>,
  )
}
