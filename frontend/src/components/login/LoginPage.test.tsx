import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import { LoginPage } from './LoginPage'

type Call = { method: string; path: string; body: unknown }

let calls: Call[]
let pollsUntilComplete: number
let verifyingPolls: number
let knownName: string | null

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status }))
}

beforeEach(() => {
  calls = []
  pollsUntilComplete = 1
  verifyingPolls = 0
  knownName = 'Mari'

  vi.mocked(fetch).mockImplementation((input, init) => {
    const path = String(input)
    const method = init?.method ?? 'GET'
    calls.push({ method, path, body: init?.body ? JSON.parse(String(init.body)) : undefined })
    if (method === 'POST' && path === '/v1/auth/mobile-id') {
      return json({ loginId: 'login-1', verificationCode: '1234', firstName: knownName })
    }
    if (method === 'POST' && path === '/v1/auth/smart-id') {
      return json({ loginId: 'login-1', verificationCode: '5678', firstName: knownName })
    }
    if (method === 'GET' && path === '/v1/auth/logins/login-1') {
      pollsUntilComplete -= 1
      if (pollsUntilComplete >= 0) {
        return json({ status: 'PENDING', firstName: null })
      }
      if (verifyingPolls > 0) {
        verifyingPolls -= 1
        return json({ status: 'VERIFYING', firstName: null })
      }
      return json({ status: 'COMPLETE', firstName: 'Mari' })
    }
    if (method === 'DELETE') {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    return json({}, 401)
  })
})

function renderLogin() {
  return renderWithProviders(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
}

const success = { timeout: 4000 }

describe('LoginPage', () => {
  it('offers Smart-ID and Mobile-ID only', () => {
    renderLogin()

    expect(screen.getByRole('heading', { name: 'Tuvasta end' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Smart-ID' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Mobiil-ID' })).toBeInTheDocument()
    expect(screen.getAllByRole('tab')).toHaveLength(2)
  })

  it('hides the Mobile-ID code once it is confirmed on the phone, then logs in', async () => {
    pollsUntilComplete = 0
    verifyingPolls = 1
    renderLogin()
    await userEvent.click(screen.getByRole('tab', { name: 'Mobiil-ID' }))
    await userEvent.type(screen.getByLabelText('Isikukood'), '60001017869')
    await userEvent.type(screen.getByLabelText('Telefoninumber'), '68000769')
    await userEvent.click(screen.getByRole('button', { name: 'Sisenen' }))

    expect(await screen.findByTestId('verification-code')).toHaveTextContent('1234')
    expect(
      await screen.findByText('Kinnitus saadud, logime sisse…', {}, success),
    ).toBeInTheDocument()
    expect(screen.queryByTestId('verification-code')).not.toBeInTheDocument()
    await waitFor(() => expect(screen.getByText(/Mari/)).toBeInTheDocument(), success)
  })

  it('logs in with Mobile-ID after showing the verification code', async () => {
    renderLogin()
    await userEvent.click(screen.getByRole('tab', { name: 'Mobiil-ID' }))
    await userEvent.type(screen.getByLabelText('Isikukood'), '60001017869')
    await userEvent.type(screen.getByLabelText('Telefoninumber'), '68000769')
    await userEvent.click(screen.getByRole('button', { name: 'Sisenen' }))

    expect(await screen.findByTestId('verification-code')).toHaveTextContent('1234')
    expect(screen.getByText('Veendu, et telefonis on see kood ja nimi Tuleva:')).toBeInTheDocument()
    expect(calls[0].body).toEqual({ personalCode: '60001017869', phoneNumber: '+37268000769' })

    expect(await screen.findByRole('link', { name: 'Jätkan' }, success)).toHaveAttribute(
      'href',
      '/laen',
    )
    expect(screen.getByRole('heading', { name: 'Tere, Mari!' })).toBeInTheDocument()
  })

  it('logs in with Smart-ID by personal code, greeting the person by name meanwhile', async () => {
    renderLogin()
    await userEvent.type(screen.getByLabelText('Isikukood'), '60001017869')
    await userEvent.click(screen.getByRole('button', { name: 'Sisenen' }))

    expect(await screen.findByTestId('verification-code')).toHaveTextContent('5678')
    expect(screen.getByTestId('login-greeting')).toHaveTextContent('Tere, Mari!')
    expect(calls[0]).toMatchObject({
      path: '/v1/auth/smart-id',
      body: { personalCode: '60001017869' },
    })
    expect(await screen.findByRole('link', { name: 'Jätkan' }, success)).toBeInTheDocument()
  })

  it('shows no greeting when the name is not known', async () => {
    knownName = null
    pollsUntilComplete = 100
    renderLogin()
    await userEvent.type(screen.getByLabelText('Isikukood'), '60001017869')
    await userEvent.click(screen.getByRole('button', { name: 'Sisenen' }))

    expect(await screen.findByTestId('verification-code')).toHaveTextContent('5678')
    expect(screen.queryByTestId('login-greeting')).not.toBeInTheDocument()
  })

  it('cancels a pending login', async () => {
    pollsUntilComplete = 100
    renderLogin()
    await userEvent.click(screen.getByRole('tab', { name: 'Mobiil-ID' }))
    await userEvent.type(screen.getByLabelText('Isikukood'), '60001017869')
    await userEvent.type(screen.getByLabelText('Telefoninumber'), '68000769')
    await userEvent.click(screen.getByRole('button', { name: 'Sisenen' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Katkestan' }))

    expect(screen.getByRole('heading', { name: 'Tuvasta end' })).toBeInTheDocument()
    await waitFor(() =>
      expect(calls).toContainEqual({
        method: 'DELETE',
        path: '/v1/auth/logins/login-1',
        body: undefined,
      }),
    )
  })

  it('checks the personal code before contacting the server', async () => {
    renderLogin()
    await userEvent.click(screen.getByRole('tab', { name: 'Mobiil-ID' }))
    await userEvent.type(screen.getByLabelText('Isikukood'), '60001017860')
    await userEvent.type(screen.getByLabelText('Telefoninumber'), '68000769')
    await userEvent.click(screen.getByRole('button', { name: 'Sisenen' }))

    expect(screen.getByText('Kontrolli isikukoodi.')).toBeInTheDocument()
    expect(calls).toHaveLength(0)
  })
})
