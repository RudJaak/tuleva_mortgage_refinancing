import { FormattedMessage, useIntl } from 'react-intl'
import { useCommitment } from '../commitment/api'
import { PoolTracker } from '../pool/PoolTracker'

export function DonePage() {
  const intl = useIntl()
  const commitment = useCommitment()
  const signed = commitment.data?.committed ? commitment.data : null
  return (
    <section className="d-flex flex-column gap-4">
      <div>
        <h1 className="text-navy">
          <FormattedMessage id="done.heading" />
        </h1>
        <p className="lead mb-0">
          <FormattedMessage id="done.lead" />
        </p>
      </div>
      {signed && (
        <p className="mb-0" data-testid="done-commitment">
          <FormattedMessage
            id="done.commitment"
            values={{
              balance: intl.formatNumber(signed.committedBalance!, {
                style: 'currency',
                currency: 'EUR',
                maximumFractionDigits: 0,
              }),
              margin: intl.formatNumber(signed.currentMargin! / 100, {
                style: 'percent',
                minimumFractionDigits: 2,
              }),
            }}
          />
        </p>
      )}
      <PoolTracker />
    </section>
  )
}
