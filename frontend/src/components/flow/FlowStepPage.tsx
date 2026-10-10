import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { Placeholder } from '../layout/Placeholder'
import { flowSteps, paths, type FlowStep } from '../paths'
import { ReviewStep } from '../commitment/ReviewStep'
import { LoanStep } from '../loan/LoanStep'
import { StatementsStep } from '../statements/StatementsStep'
import { WhyWeAsk } from './WhyWeAsk'

export function FlowStepPage({ section }: { section: FlowStep['section'] }) {
  const index = flowSteps.findIndex((step) => step.section === section)
  const previous = index > 0 ? flowSteps[index - 1].path : paths.login
  const next = index < flowSteps.length - 1 ? flowSteps[index + 1].path : paths.done
  return (
    <section className="d-flex flex-column gap-4">
      <div>
        <h1 className="text-navy">
          <FormattedMessage id={`flow.${section}.heading`} />
        </h1>
        <p className="lead mb-0">
          <FormattedMessage id={`flow.${section}.lead`} />
        </p>
      </div>
      <WhyWeAsk section={section} />
      {section === 'loan' ? (
        <LoanStep />
      ) : section === 'statements' ? (
        <StatementsStep />
      ) : section === 'review' ? (
        <ReviewStep />
      ) : (
        <Placeholder>
          <FormattedMessage id="flow.placeholder" />
        </Placeholder>
      )}
      <div className="d-flex gap-2">
        <Link to={previous} className="btn btn-light">
          <FormattedMessage id="flow.back" />
        </Link>
        {section !== 'review' && (
          <Link to={next} className="btn btn-primary">
            <FormattedMessage id="flow.next" />
          </Link>
        )}
      </div>
    </section>
  )
}
