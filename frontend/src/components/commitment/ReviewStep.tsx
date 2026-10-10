import { useEffect, useState, type FormEvent } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { Loader } from '../common/Loader'
import { useLoan } from '../loan/api'
import { CreditCheckCard } from '../creditcheck/CreditCheckCard'
import { useCreditCheck } from '../creditcheck/api'
import { useMe } from '../login/api'
import { VerificationCodeLoader } from '../login/VerificationCodeLoader'
import { paths } from '../paths'
import { useStatementDocuments, useStatementsOverview } from '../statements/api'
import { useCommitment } from './api'
import { useSigning } from './useSigning'

/**
 * The last step: the borrower reviews their loan and statements, reads the conditional commitment,
 * consents to the data sharing and signs with emulated Smart-ID or Mobile-ID.
 */
export function ReviewStep() {
  const intl = useIntl()
  const navigate = useNavigate()
  const loan = useLoan()
  const documents = useStatementDocuments()
  const overview = useStatementsOverview(documents.data)
  const commitment = useCommitment()
  const { state, sign, cancel } = useSigning()
  const me = useMe()
  const creditCheck = useCreditCheck()
  const [consent, setConsent] = useState(false)
  // Signing reuses the login's method; the backend keeps it (and the Mobile-ID phone) in the session
  const method = me.data?.loginMethod ?? 'SMART_ID'

  useEffect(() => {
    if (state.phase === 'signed') {
      navigate(paths.done)
    }
  }, [state.phase, navigate])

  if ([loan.error, commitment.error].some((e) => e instanceof ApiError && e.status === 401)) {
    return (
      <p className="alert alert-info mb-0">
        <FormattedMessage
          id="documents.loginRequired"
          values={{ link: (text) => <Link to={paths.login}>{text}</Link> }}
        />
      </p>
    )
  }
  if (loan.isPending || commitment.isPending) {
    return <Loader />
  }

  const euro = (amount: number) =>
    intl.formatNumber(amount, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })
  const percent = (value: number) =>
    intl.formatNumber(value / 100, { style: 'percent', minimumFractionDigits: 2 })

  if (commitment.data?.committed) {
    return (
      <div className="card rounded-4 p-4 d-flex flex-column gap-2" data-testid="already-committed">
        <h2 className="h5 text-navy mb-0">
          <FormattedMessage id="review.committed.heading" />
        </h2>
        <p className="mb-0">
          <FormattedMessage
            id="review.committed.lead"
            values={{
              date: intl.formatDate(commitment.data.signedAt!, { dateStyle: 'long' }),
              margin: percent(commitment.data.currentMargin!),
            }}
          />
        </p>
        <Link to={paths.done} className="btn btn-primary align-self-start mt-2">
          <FormattedMessage id="review.committed.continue" />
        </Link>
      </div>
    )
  }

  if (state.phase === 'pending') {
    return (
      <section
        className="card rounded-4 p-4 p-sm-5 text-center mx-auto login-page"
        aria-label={intl.formatMessage({ id: 'review.sign.pending' })}
        data-testid="pending-signature"
      >
        <VerificationCodeLoader
          verificationCode={state.verificationCode}
          verifying={state.verifying}
          onCancel={cancel}
        />
      </section>
    )
  }

  const confirmed = loan.data ?? null
  const summary = confirmed?.summary
  const household = overview.data?.household
  const hasStatements = (overview.data?.persons ?? []).some((p) => p.documentIds.length > 0)
  const loanReady =
    confirmed !== null && confirmed.terms.margin !== null && summary?.balance != null

  const submit = (event: FormEvent) => {
    event.preventDefault()
    sign()
  }

  return (
    <div className="d-flex flex-column gap-4">
      <section className="card rounded-4 p-4 d-flex flex-column gap-2" data-testid="review-loan">
        <div className="d-flex justify-content-between gap-2">
          <h2 className="h5 text-navy mb-0">
            <FormattedMessage id="review.loan.heading" />
          </h2>
          <Link to={paths.loan} className="small">
            <FormattedMessage id="review.change" />
          </Link>
        </div>
        {loanReady ? (
          <ul className="list-unstyled mb-0">
            <li>
              <FormattedMessage
                id="review.loan.lender"
                values={{ lender: confirmed.terms.lender ?? '—' }}
              />
            </li>
            <li>
              <FormattedMessage
                id="review.loan.margin"
                values={{ margin: percent(confirmed.terms.margin!) }}
              />
            </li>
            <li>
              <FormattedMessage
                id={
                  summary!.balanceSource === 'ESTIMATED'
                    ? 'review.loan.balanceEstimated'
                    : 'review.loan.balance'
                }
                values={{ balance: euro(summary!.balance!) }}
              />
            </li>
            {summary!.remainingMonths !== null && (
              <li>
                <FormattedMessage
                  id="review.loan.remaining"
                  values={{
                    years: Math.floor(summary!.remainingMonths / 12),
                    months: summary!.remainingMonths % 12,
                  }}
                />
              </li>
            )}
          </ul>
        ) : (
          <p className="mb-0 text-danger" role="alert">
            <FormattedMessage
              id="review.loan.missing"
              values={{ link: (text) => <Link to={paths.loan}>{text}</Link> }}
            />
          </p>
        )}
      </section>

      <section
        className="card rounded-4 p-4 d-flex flex-column gap-2"
        data-testid="review-statements"
      >
        <div className="d-flex justify-content-between gap-2">
          <h2 className="h5 text-navy mb-0">
            <FormattedMessage id="review.statements.heading" />
          </h2>
          <Link to={paths.statements} className="small">
            <FormattedMessage id="review.change" />
          </Link>
        </div>
        {hasStatements && household ? (
          <p className="mb-0">
            <FormattedMessage
              id="statements.totals"
              values={{
                income: euro(household.monthlyIncome),
                obligations: euro(household.monthlyObligations),
                ratio:
                  household.debtServiceRatio === null
                    ? '—'
                    : intl.formatNumber(household.debtServiceRatio, { style: 'percent' }),
              }}
            />
          </p>
        ) : (
          <p className="mb-0 text-body-secondary">
            <FormattedMessage
              id="review.statements.missing"
              values={{ link: (text) => <Link to={paths.statements}>{text}</Link> }}
            />
          </p>
        )}
      </section>

      <CreditCheckCard />

      <form className="card rounded-4 p-4 d-flex flex-column gap-3" onSubmit={submit} noValidate>
        <h2 className="h5 text-navy mb-0">
          <FormattedMessage id="review.commitment.heading" />
        </h2>
        <p className="mb-0" data-testid="commitment-text">
          <FormattedMessage
            id="review.commitment.text"
            values={{
              margin: confirmed?.terms.margin != null ? percent(confirmed.terms.margin) : '—',
            }}
          />
        </p>
        <p className="small text-body-secondary mb-0">
          <FormattedMessage id="review.commitment.conditions" />
        </p>
        <div className="form-check">
          <input
            id="review-consent"
            className="form-check-input"
            type="checkbox"
            checked={consent}
            onChange={(event) => setConsent(event.target.checked)}
          />
          <label className="form-check-label" htmlFor="review-consent">
            <FormattedMessage id="review.consent" />
          </label>
        </div>

        {state.phase === 'idle' && state.errorReason && (
          <p className="alert alert-danger mb-0" role="alert">
            <FormattedMessage id={signingErrorId(state.errorReason)} />
          </p>
        )}
        <div>
          <button
            type="submit"
            className="btn btn-primary btn-lg"
            disabled={!consent || !loanReady || !creditCheck.data}
          >
            <FormattedMessage id={`review.sign.submit.${method}`} />
          </button>
        </div>
      </form>
    </div>
  )
}

const knownReasons = [
  'LOAN_NOT_CONFIRMED',
  'CREDIT_CHECK_MISSING',
  'NO_POSSIBLE_GAIN',
  'ALREADY_COMMITTED',
  'PHONE_NUMBER_INVALID',
]

function signingErrorId(reason: string) {
  return knownReasons.includes(reason) ? `review.error.${reason}` : 'review.error.GENERIC'
}
