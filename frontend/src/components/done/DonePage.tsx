import { FormattedMessage } from 'react-intl'

export function DonePage() {
  return (
    <section className="d-flex flex-column gap-3">
      <h1 className="text-navy">
        <FormattedMessage id="done.heading" />
      </h1>
      <p className="lead">
        <FormattedMessage id="done.lead" />
      </p>
    </section>
  )
}
