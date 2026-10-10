import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import type { StatementDocument } from './api'
import { inProgress, processed } from './fixtures'
import { StatementsStep } from './StatementsStep'

const listPath = '/v1/documents?type=ACCOUNT_STATEMENT'

function mockDocuments(
  list: StatementDocument[] | { status: number },
  upload?: { status: number; body: unknown },
) {
  vi.mocked(fetch).mockImplementation((input, init) => {
    const path = String(input)
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
      await screen.findByLabelText('Lisa väljavõte'),
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
      await screen.findByLabelText('Lisa väljavõte'),
      new File(['%PDF'], 'statement.pdf', { type: 'application/pdf' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Lisa panga digitempliga väljavõte (.asice).',
    )
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
