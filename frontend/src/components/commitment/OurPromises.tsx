import { FormattedMessage } from 'react-intl'

const weDo = ['verify', 'bargain', 'anonymise', 'notify', 'delete']
const weDont = ['contract', 'documents', 'identify', 'decide']

/** What joining means in practice: what the organiser does for the borrower, and what it doesn't. */
export function OurPromises() {
  return (
    <section className="card rounded-4 p-4 d-flex flex-column gap-3" data-testid="our-promises">
      <h2 className="h5 text-navy mb-0">
        <FormattedMessage id="review.promises.heading" />
      </h2>
      <div className="row gy-3">
        <PromiseList kind="do" items={weDo} />
        <PromiseList kind="dont" items={weDont} />
      </div>
    </section>
  )
}

function PromiseList({ kind, items }: { kind: 'do' | 'dont'; items: string[] }) {
  return (
    <div className="col-lg-6">
      <h3 className="h6 text-navy">
        <FormattedMessage id={`review.promises.${kind}.heading`} />
      </h3>
      <ul className="list-unstyled mb-0 d-flex flex-column gap-2">
        {items.map((item) => (
          <li key={item} className="d-flex align-items-start gap-2">
            <span className={`promise-mark promise-mark-${kind}`} aria-hidden="true">
              {kind === 'do' ? '✓' : '✕'}
            </span>
            <span>
              <FormattedMessage id={`review.promises.${kind}.${item}`} />
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
