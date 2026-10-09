import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { Placeholder } from '../layout/Placeholder'
import { paths } from '../paths'

export function LoginPage() {
  return (
    <div className="d-flex flex-column gap-4 mx-auto" style={{ maxWidth: '30rem' }}>
      <h1 className="text-navy">
        <FormattedMessage id="login.heading" />
      </h1>
      <p className="lead">
        <FormattedMessage id="login.lead" />
      </p>
      <Placeholder>
        <FormattedMessage id="login.placeholder" />
      </Placeholder>
      <div>
        <Link to={paths.loan} className="btn btn-primary">
          <FormattedMessage id="flow.next" />
        </Link>
      </div>
    </div>
  )
}
