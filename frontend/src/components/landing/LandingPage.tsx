import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { SavingsCalculator } from '../calculator/SavingsCalculator'
import { paths } from '../paths'
import { PoolTracker } from '../pool/PoolTracker'

/** The pool's momentum first, then what it could mean for you, then the way in. */
export function LandingPage() {
  return (
    <div className="d-flex flex-column gap-5">
      <section className="d-flex flex-column gap-3">
        <h1 className="text-navy mb-0">
          <FormattedMessage id="landing.heading" />
        </h1>
        <p className="lead mb-0">
          <FormattedMessage id="landing.lead" />
        </p>
      </section>
      <PoolTracker />
      <section className="row justify-content-center">
        <div className="col-lg-8">
          <SavingsCalculator />
        </div>
      </section>
      <section className="d-flex flex-column align-items-center gap-2 text-center">
        <Link to={paths.login} className="btn btn-primary btn-lg px-5">
          <FormattedMessage id="landing.cta" />
        </Link>
      </section>
    </div>
  )
}
