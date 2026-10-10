import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import { LoginPage } from './LoginPage'

type Call = { method: string; path: string; body: unknown }

let calls: Call[]
let pollsUntilComplete: number

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status }))
}

beforeEach(() => {
  calls = []
  pollsUntilComplete = 1
  vi.mocked(fetch).mockImplementation((input, init) => {
    const path = String(input)
    const method = init?.method ?? 'GET'
    calls.push({ method, path, body: init?.body ? JSON.parse(String(init.body)) : undefined })
    if (method === 'POST' && path === '/v1/auth/mobile-id') {
      return json({ loginId: 'login-1', verificationCode: '1234', deviceLink: null })
    }
    if (method === 'POST' && path === '/v1/auth/smart-id') {
      return json({
        loginId: 'login-1',
        verificationCode: null,
        deviceLink: 'https://smart-id.com/demo',
      })
    }
    if (method === 'POST' && path === '/v1/auth/id-card') {
      return json({ loginId: 'login-1', verificationCode: null, deviceLink: null })
    }
    if (method === 'GET' && path === '/v1/auth/logins/login-1') {
      pollsUntilComplete -= 1
      return json(
        pollsUntilComplete < 0
          ? { status: 'COMPLETE', firstName: 'Mari' }
          : { status: 'PENDING', firstName: null },
      )
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
  it('offers Smart-ID, Mobile-ID and ID-card', () => {
    renderLogin()

    expect(screen.getByRole('heading', { name: 'Tuvasta end' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Smart-ID' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Mobiil-ID' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'ID-kaart' })).toBeInTheDocument()
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

    expect(await screen.findByRole('heading', { name: 'Tere, Mari!' }, success)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Jätkan' })).toHaveAttribute('href', '/laen')
  })

  it('shows the Smart-ID QR code while waiting', async () => {
    renderLogin()
    await userEvent.click(screen.getByRole('button', { name: /Sisenen/ }))

    expect(
      await screen.findByRole('img', { name: 'Skänni Smart‑ID rakendusega' }),
    ).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Tere, Mari!' }, success)).toBeInTheDocument()
  })

  it('logs in with an ID-card', async () => {
    renderLogin()
    await userEvent.click(screen.getByRole('tab', { name: 'ID-kaart' }))
    await userEvent.click(screen.getByRole('button', { name: 'Sisenen' }))

    expect(await screen.findByRole('heading', { name: 'Tere, Mari!' }, success)).toBeInTheDocument()
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
