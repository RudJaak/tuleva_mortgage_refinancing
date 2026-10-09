import { FormattedMessage } from 'react-intl'
import { Outlet } from 'react-router-dom'
import { Header } from './Header'

export function Layout() {
  return (
    <>
      <a className="skip-link visually-hidden-focusable" href="#main">
        <FormattedMessage id="app.skipToContent" />
      </a>
      <Header />
      <main id="main" className="container pb-6">
        <Outlet />
      </main>
    </>
  )
}
