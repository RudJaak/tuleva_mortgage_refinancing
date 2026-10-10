import { FormattedMessage } from 'react-intl'
import type { FlowStep } from '../paths'

/**
 * Why a step asks for something and what happens to it, in two short paragraphs. The wording
 * describes the intended launch setup: documents are read by a model running on our own server.
 */
export function WhyWeAsk({ section }: { section: FlowStep['section'] }) {
  return (
    <aside
      className="why-we-ask card rounded-4 p-3 p-sm-4 bg-blue-2 border-0"
      data-testid="why-we-ask"
    >
      <div className="row g-3">
        <div className="col-md-6">
          <h2 className="h6 text-navy mb-1">
            <FormattedMessage id="flow.why.purpose" />
          </h2>
          <p className="small mb-0">
            <FormattedMessage id={`flow.${section}.why.purpose`} />
          </p>
        </div>
        <div className="col-md-6">
          <h2 className="h6 text-navy mb-1">
            <FormattedMessage id="flow.why.data" />
          </h2>
          <p className="small mb-0">
            <FormattedMessage id={`flow.${section}.why.data`} />
          </p>
        </div>
      </div>
    </aside>
  )
}
