import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { paths } from '../paths'

export function NotFoundPage() {
  return (
    <section className="d-flex flex-column gap-3">
      <h1 className="text-navy">
        <FormattedMessage id="notFound.heading" />
      </h1>
      <Link to={paths.landing}>
        <FormattedMessage id="notFound.back" />
      </Link>
    </section>
  )
}
