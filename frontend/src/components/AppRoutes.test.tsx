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
    [paths.login, 'Tuvasta end'],
    [paths.loan, 'Sinu kodulaen'],
    [paths.statements, 'Kontoväljavõtted'],
    [paths.review, 'Kontrolli ja kinnita'],
    [paths.done, 'Oled ühispakkumises'],
    ['/olematu', 'Lehte ei leitud'],
  ])('renders %s in Estonian', (path, heading) => {
    renderAt(path)

    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
  })

  it('marks the current flow step', () => {
    renderAt(paths.statements)

    const current = screen
      .getAllByRole('listitem')
      .find((item) => item.getAttribute('aria-current') === 'step')
    expect(current).toHaveTextContent('Väljavõtted')
  })

  it.each([paths.loan, paths.statements, paths.review])(
    'explains on %s why it asks and what happens to the data',
    (path) => {
      renderAt(path)

      expect(screen.getByRole('heading', { name: 'Miks me seda küsime' })).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: 'Mis sinu andmetega juhtub' })).toBeInTheDocument()
      expect(screen.getByTestId('why-we-ask')).toHaveTextContent(/meie|meie serveris|Pangad/)
    },
  )
})
