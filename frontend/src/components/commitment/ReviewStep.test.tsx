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

function mockBackend({
  loan = confirmedLoan as unknown,
  commitment = notCommitted,
  signing = { status: 200, body: { signingId: 's1', verificationCode: '4321', firstName: 'Mari' } },
}: {
  loan?: unknown
  commitment?: CommitmentView
  signing?: { status: number; body: unknown }
} = {}) {
  const posts: unknown[] = []
  vi.mocked(fetch).mockImplementation((input, init) => {
    const path = String(input)
    const reply = (body: unknown, status = 200) =>
      Promise.resolve(new Response(body === null ? null : JSON.stringify(body), { status }))
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
    const submit = screen.getByRole('button', { name: 'Allkirjastan ja liitun' })
    expect(submit).toBeDisabled()

    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(submit)

    expect(await screen.findByTestId('verification-code')).toHaveTextContent('4321')
    expect(posts).toEqual([{ method: 'SMART_ID' }])
    expect(await screen.findByText('Valmis-leht', {}, { timeout: 3000 })).toBeInTheDocument()
  })

  it('signs with Mobile-ID using the phone number', async () => {
    const posts = mockBackend()
    renderStep()

    await userEvent.click(await screen.findByRole('checkbox'))
    await userEvent.click(screen.getByLabelText('Mobiil-ID'))
    await userEvent.type(screen.getByLabelText('Telefoninumber'), '55512345')
    await userEvent.click(screen.getByRole('button', { name: 'Allkirjastan ja liitun' }))

    await screen.findByTestId('verification-code')
    expect(posts).toEqual([{ method: 'MOBILE_ID', phoneNumber: '+37255512345' }])
  })

  it('asks for the loan before anyone can sign', async () => {
    mockBackend({ loan: null })
    renderStep()

    expect(await screen.findByRole('alert')).toHaveTextContent('Kinnita enne oma laenu andmed')
    await userEvent.click(screen.getByRole('checkbox'))
    expect(screen.getByRole('button', { name: 'Allkirjastan ja liitun' })).toBeDisabled()
  })

  it('explains why a signature was refused', async () => {
    mockBackend({ signing: { status: 422, body: { reason: 'NO_POSSIBLE_GAIN' } } })
    renderStep()

    await userEvent.click(await screen.findByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: 'Allkirjastan ja liitun' }))

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
    expect(screen.queryByRole('button', { name: 'Allkirjastan ja liitun' })).not.toBeInTheDocument()
  })
})
