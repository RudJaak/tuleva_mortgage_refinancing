import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import { confirmedLoan } from '../loan/fixtures'
import type { CommitmentView } from './api'
import { ReviewStep } from './ReviewStep'

const notCommitted: CommitmentView = {
  committed: false,
  committedBalance: null,
  currentMargin: null,
  signedAt: null,
}

const clearCheck = {
  result: 'CLEAR',
  activeDefaults: 0,
  source: 'Creditinfo maksehäireregister (näidis)',
  checkedAt: '2026-10-10T12:00:00Z',
}

function mockBackend({
  creditCheck = clearCheck as unknown,
  loginMethod = 'SMART_ID',
  loan = confirmedLoan as unknown,
  commitment = notCommitted,
  signing = { status: 200, body: { signingId: 's1', verificationCode: '4321', firstName: 'Mari' } },
}: {
  creditCheck?: unknown
  loginMethod?: 'SMART_ID' | 'MOBILE_ID'
  loan?: unknown
  commitment?: CommitmentView
  signing?: { status: number; body: unknown }
} = {}) {
  const posts: unknown[] = []
  vi.mocked(fetch).mockImplementation((input, init) => {
    const path = String(input)
    const reply = (body: unknown, status = 200) =>
      Promise.resolve(new Response(body === null ? null : JSON.stringify(body), { status }))
    if (path === '/v1/me/credit-check' && init?.method === 'POST') {
      return reply(clearCheck)
    }
    if (path === '/v1/me/credit-check') {
      return creditCheck ? reply(creditCheck) : reply(null, 404)
    }
    if (path === '/v1/me') {
      return reply({ firstName: 'Mari', lastName: 'Tamm', loginMethod })
    }
    if (path === '/v1/me/loan') {
      return loan ? reply(loan) : reply(null, 404)
    }
    if (path === '/v1/me/commitment') {
      return reply(commitment)
    }
    if (path === '/v1/me/commitment/signing' && init?.method === 'POST') {
      posts.push(JSON.parse(String(init.body)))
      return reply(signing.body, signing.status)
    }
    if (path === '/v1/me/commitment/signing/s1') {
      return reply({ status: 'COMPLETE' })
    }
    if (path === '/v1/documents?type=ACCOUNT_STATEMENT') {
      return reply([])
    }
    if (path === '/v1/me/statements') {
      return reply({ persons: [], household: null })
    }
    return Promise.reject(new Error(`Unmocked ${path}`))
  })
  return posts
}

function renderStep() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/kinnitus']}>
      <Routes>
        <Route path="/kinnitus" element={<ReviewStep />} />
        <Route path="/valmis" element={<p>Valmis-leht</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ReviewStep', () => {
  it('shows the loan and the conditional commitment, and signs only after consent', async () => {
    const posts = mockBackend()
    renderStep()

    const loan = await screen.findByTestId('review-loan')
    expect(within(loan).getByText('Marginaal: 2,95%')).toBeInTheDocument()
    expect(screen.getByTestId('commitment-text')).toHaveTextContent(
      'madalam kui minu praegune marginaal 2,95%',
    )
    expect(screen.getByText(/Väljavõtteid pole lisatud/)).toBeInTheDocument()
    const submit = screen.getByRole('button', { name: 'Allkirjastan Smart-ID-ga' })
    expect(submit).toBeDisabled()

    await userEvent.click(screen.getByRole('checkbox', { name: /Nõustun, et minu laenu/ }))
    await userEvent.click(submit)

    expect(await screen.findByTestId('verification-code')).toHaveTextContent('4321')
    expect(posts).toEqual([{}])
    expect(await screen.findByText('Valmis-leht', {}, { timeout: 3000 })).toBeInTheDocument()
  })

  it('signs with Mobile-ID when that is how the borrower logged in', async () => {
    const posts = mockBackend({ loginMethod: 'MOBILE_ID' })
    renderStep()

    await userEvent.click(await screen.findByRole('checkbox', { name: /Nõustun, et minu laenu/ }))
    await userEvent.click(await screen.findByRole('button', { name: 'Allkirjastan Mobiil-ID-ga' }))

    expect(await screen.findByTestId('verification-code')).toHaveTextContent('4321')
    expect(posts).toEqual([{}])
    expect(screen.queryByLabelText('Telefoninumber')).not.toBeInTheDocument()
  })

  it('runs the payment-default check with consent before signing is possible', async () => {
    mockBackend({ creditCheck: null })
    renderStep()

    await userEvent.click(await screen.findByRole('checkbox', { name: /Nõustun, et minu laenu/ }))
    expect(screen.getByRole('button', { name: 'Allkirjastan Smart-ID-ga' })).toBeDisabled()

    const card = screen.getByTestId('credit-check')
    expect(within(card).getByRole('button', { name: 'Kontrolli maksehäireid' })).toBeDisabled()
    await userEvent.click(within(card).getByRole('checkbox'))
    await userEvent.click(within(card).getByRole('button', { name: 'Kontrolli maksehäireid' }))

    expect(await within(card).findByText('Kehtivaid maksehäireid ei leitud')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Allkirjastan Smart-ID-ga' })).toBeEnabled()
  })

  it('asks for the loan before anyone can sign', async () => {
    mockBackend({ loan: null })
    renderStep()

    expect(await screen.findByRole('alert')).toHaveTextContent('Kinnita enne oma laenu andmed')
    await userEvent.click(screen.getByRole('checkbox', { name: /Nõustun, et minu laenu/ }))
    expect(screen.getByRole('button', { name: 'Allkirjastan Smart-ID-ga' })).toBeDisabled()
  })

  it('explains why a signature was refused', async () => {
    mockBackend({ signing: { status: 422, body: { reason: 'NO_POSSIBLE_GAIN' } } })
    renderStep()

    await userEvent.click(await screen.findByRole('checkbox', { name: /Nõustun, et minu laenu/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Allkirjastan Smart-ID-ga' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('parimast eesmärgist madalam')
  })

  it('shows an existing commitment instead of the form', async () => {
    mockBackend({
      commitment: {
        committed: true,
        committedBalance: 150000,
        currentMargin: 1.85,
        signedAt: '2026-10-10T12:00:00Z',
      },
    })
    renderStep()

    expect(await screen.findByTestId('already-committed')).toHaveTextContent('marginaaliga 1,85%')
    expect(screen.queryByRole('button', { name: /Allkirjastan/ })).not.toBeInTheDocument()
  })
})
