import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { Placeholder } from '../layout/Placeholder'
import { paths } from '../paths'

export function LandingPage() {
  return (
    <div className="d-flex flex-column gap-5">
      <section className="d-flex flex-column gap-3">
        <h1 className="text-navy">
          <FormattedMessage id="landing.heading" />
        </h1>
        <p className="lead">
          <FormattedMessage id="landing.lead" />
        </p>
        <div>
          <Link to={paths.login} className="btn btn-primary btn-lg">
            <FormattedMessage id="landing.cta" />
          </Link>
        </div>
      </section>
      <Placeholder>
        <FormattedMessage id="landing.calculator.placeholder" />
      </Placeholder>
      <Placeholder>
        <FormattedMessage id="landing.tracker.placeholder" />
      </Placeholder>
      <Placeholder>
        <FormattedMessage id="landing.content.placeholder" />
      </Placeholder>
    </div>
  )
}
