import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AppProviders } from './AppProviders'
import { AppRoutes } from './AppRoutes'
import { paths } from './paths'

function renderAt(path: string) {
  render(
    <AppProviders>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </AppProviders>,
  )
}

describe('AppRoutes', () => {
  it.each([
    [paths.landing, 'Koos saame kodulaenule parema marginaali'],
    [paths.login, 'Sisene'],
    [paths.loan, 'Sinu kodulaen'],
    [paths.property, 'Sinu kodu'],
    [paths.household, 'Leibkond ja kohustused'],
    [paths.statements, 'Kontoväljavõtted'],
    [paths.review, 'Kontrolli ja kinnita'],
    [paths.done, 'Oled ühispakkumises'],
    ['/olematu', 'Lehte ei leitud'],
  ])('renders %s in Estonian', (path, heading) => {
    renderAt(path)

    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
  })

  it('marks the current flow step', () => {
    renderAt(paths.household)

    const current = screen
      .getAllByRole('listitem')
      .find((item) => item.getAttribute('aria-current') === 'step')
    expect(current).toHaveTextContent('Leibkond')
  })
})
