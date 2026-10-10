import { useState } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { Loader } from '../common/Loader'
import { useCreditCheck, useRunCreditCheck } from './api'

/**
 * The payment-default check (D32), after the documents because each query costs money, and only
 * with the borrower's consent. Emulated in the demo: no register is actually queried.
 */
export function CreditCheckCard() {
  const intl = useIntl()
  const check = useCreditCheck()
  const run = useRunCreditCheck()
  const [consent, setConsent] = useState(false)

  return (
    <section className="card rounded-4 p-4 d-flex flex-column gap-3" data-testid="credit-check">
      <h2 className="h5 text-navy mb-0">
        <FormattedMessage id="creditCheck.heading" />
      </h2>
      {check.data ? (
        <div className="d-flex align-items-start gap-3">
          <span className="membership-mark" aria-hidden="true">
            {check.data.result === 'CLEAR' ? '✓' : '!'}
          </span>
          <div>
            <div
              className={`fw-bold ${check.data.result === 'CLEAR' ? 'text-success' : 'text-danger'}`}
            >
              <FormattedMessage
                id={`creditCheck.result.${check.data.result}`}
                values={{ count: check.data.activeDefaults }}
              />
            </div>
            <div className="small text-body-secondary">
              <FormattedMessage
                id="creditCheck.checked"
                values={{
                  source: check.data.source,
                  date: intl.formatDate(check.data.checkedAt, {
                    dateStyle: 'short',
                    timeStyle: 'short',
                  }),
                }}
              />
            </div>
          </div>
        </div>
      ) : run.isPending ? (
        <div className="d-flex align-items-center gap-3">
          <Loader
            className="membership-loader"
            label={intl.formatMessage({ id: 'creditCheck.running' })}
          />
          <span className="text-body-secondary">
            <FormattedMessage id="creditCheck.running" />
          </span>
        </div>
      ) : (
        <>
          <p className="small mb-0">
            <FormattedMessage id="creditCheck.lead" />
          </p>
          <div className="form-check">
            <input
              id="credit-check-consent"
              className="form-check-input"
              type="checkbox"
              checked={consent}
              onChange={(event) => setConsent(event.target.checked)}
            />
            <label className="form-check-label" htmlFor="credit-check-consent">
              <FormattedMessage id="creditCheck.consent" />
            </label>
          </div>
          {run.isError && (
            <p className="alert alert-danger mb-0" role="alert">
              <FormattedMessage id="creditCheck.failed" />
            </p>
          )}
          <div>
            <button
              type="button"
              className="btn btn-outline-primary"
              disabled={!consent}
              onClick={() => run.mutate()}
            >
              <FormattedMessage id="creditCheck.run" />
            </button>
          </div>
        </>
      )}
    </section>
  )
}
