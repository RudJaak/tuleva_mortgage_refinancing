import { useQueryClient } from '@tanstack/react-query'
import { FormattedMessage, useIntl } from 'react-intl'
import { Link, useNavigate } from 'react-router-dom'
import logo from '../common/logo.svg'
import { logout, meQueryKey, useMe } from '../login/api'
import { paths } from '../paths'

export function Header() {
  const intl = useIntl()
  const { data: me } = useMe()
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const logOut = async () => {
    await logout().catch(() => undefined)
    queryClient.setQueryData(meQueryKey, null)
    navigate(paths.landing)
  }

  return (
    <header className="app-header container d-flex align-items-center justify-content-between py-4">
      <Link to={paths.landing} className="brand-logo">
        <img src={logo} alt={intl.formatMessage({ id: 'header.logoAlt' })} height={44} />
      </Link>
      {me && (
        <div className="d-flex align-items-center gap-3">
          <span className="text-navy fw-medium d-none d-sm-inline">
            {[me.firstName, me.lastName].filter(Boolean).join(' ')}
          </span>
          <button type="button" className="btn btn-link p-0" onClick={logOut}>
            <FormattedMessage id="header.logout" />
          </button>
        </div>
      )}
    </header>
  )
}
