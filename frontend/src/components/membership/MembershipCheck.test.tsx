import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import { MembershipCheck } from './MembershipCheck'

describe('MembershipCheck', () => {
  it('shows a spinner while checking, then confirms the membership', async () => {
    let answer: (response: Response) => void = () => undefined
    vi.mocked(fetch).mockImplementation((input) =>
      String(input) === '/v1/me/membership'
        ? new Promise((resolve) => {
            answer = resolve
          })
        : Promise.reject(new Error('unmocked')),
    )
    renderWithProviders(<MembershipCheck />)

    expect(
      await screen.findByRole('status', { name: 'Kontrollime, kas oled Tuleva liige…' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Kontrollime, kas oled Tuleva liige…')).toBeInTheDocument()
    answer(new Response(JSON.stringify({ member: true }), { status: 200 }))

    expect(await screen.findByTestId('membership-member')).toHaveTextContent(
      'Suurepärane, oled juba Tuleva liige!',
    )
    expect(screen.getByTestId('membership-member')).toHaveTextContent(/150\s€/)
  })

  it('explains the fee to someone who is not a member', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ member: false }), { status: 200 }),
    )
    renderWithProviders(<MembershipCheck />)

    expect(await screen.findByTestId('membership-not-member')).toHaveTextContent(
      /pead astuma Tuleva liikmeks.*150\s€/,
    )
  })
})
