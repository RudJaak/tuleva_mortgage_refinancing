import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { SavingsCalculator } from '../calculator/SavingsCalculator'
import { Placeholder } from '../layout/Placeholder'
import { paths } from '../paths'
import { PoolTracker } from '../pool/PoolTracker'

export function LandingPage() {
  return (
    <div className="d-flex flex-column gap-5">
      <section className="row gy-5 align-items-center">
        <div className="col-lg-6 d-flex flex-column gap-3">
          <h1 className="text-navy">
            <FormattedMessage id="landing.heading" />
          </h1>
          <p className="lead mb-0">
            <FormattedMessage id="landing.lead" />
          </p>
          <div>
            <Link to={paths.login} className="btn btn-primary btn-lg">
              <FormattedMessage id="landing.cta" />
            </Link>
          </div>
        </div>
        <div className="col-lg-6">
          <SavingsCalculator />
        </div>
      </section>
      <PoolTracker />
      <Placeholder>
        <FormattedMessage id="landing.content.placeholder" />
      </Placeholder>
    </div>
  )
}
