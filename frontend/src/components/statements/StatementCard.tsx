import { FormattedMessage, useIntl } from 'react-intl'
import { failureMessageId } from '../documents/failure'
import { ProcessingProgress } from '../documents/ProcessingProgress'
import { SealBadge } from '../documents/SealBadge'
import type { StatementDocument } from './api'
import { statementStages } from './stages'
import { StatementSummaryView } from './StatementSummaryView'

export function StatementCard({ document }: { document: StatementDocument }) {
  const intl = useIntl()
  const summary = document.result?.summary
  const date = (value: string) => intl.formatDate(value, { dateStyle: 'short' })

  return (
    <article className="card rounded-4 p-4 d-flex flex-column gap-3" data-testid="statement-card">
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
        <div>
          <h2 className="h5 text-navy mb-0">
            <FormattedMessage
              id="statements.card.heading"
              values={{ account: document.result?.account ?? document.verification.bankName ?? '' }}
            />
          </h2>
          {summary?.periodStart && summary.periodEnd && (
            <div className="small text-body-secondary">
              {date(summary.periodStart)} – {date(summary.periodEnd)}
            </div>
          )}
        </div>
        <SealBadge verification={document.verification} />
      </div>

      {document.status === 'RECEIVED' && (
        <ProcessingProgress
          createdAt={document.createdAt}
          stages={statementStages}
          labelId="statements.progress.label"
        />
      )}
      {document.status === 'PROCESSED' && summary && <StatementSummaryView summary={summary} />}
      {document.status === 'PROCESSED' && !summary && (
        <p className="mb-0 text-body-secondary">
          <FormattedMessage id="statements.card.saved" />
        </p>
      )}
      {document.status === 'FAILED' && (
        <p className="alert alert-danger mb-0" role="alert">
          <FormattedMessage id={failureMessageId(document.failure?.reason)} />
        </p>
      )}
    </article>
  )
}
