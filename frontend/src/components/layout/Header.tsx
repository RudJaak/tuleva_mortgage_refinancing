import { useIntl } from 'react-intl'
import { Link } from 'react-router-dom'
import logo from '../common/logo.svg'
import { paths } from '../paths'

export function Header() {
  const intl = useIntl()
  return (
    <header className="app-header container d-flex align-items-center py-4">
      <Link to={paths.landing} className="brand-logo">
        <img src={logo} alt={intl.formatMessage({ id: 'header.logoAlt' })} height={44} />
      </Link>
    </header>
  )
}
