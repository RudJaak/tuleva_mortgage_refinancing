import type { ReactNode } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { SavingsCalculator } from '../calculator/SavingsCalculator'
import type { LoanView } from './api'
import { LoanChecks } from './LoanChecks'

export function ConfirmedLoan({ view, onEdit }: { view: LoanView; onEdit: () => void }) {
  const intl = useIntl()
  const { terms, summary } = view
  const euro = (amount: number) =>
    intl.formatNumber(amount, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })
  const percent = (value: number, digits = 2) =>
    intl.formatNumber(value / 100, {
      style: 'percent',
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    })
  const sources = Object.values(view.provenance)
  const fromDocument = sources.filter((source) => source === 'DOCUMENT').length
  const canCalculate =
    summary.balance !== null && summary.remainingMonths !== null && terms.margin !== null

  return (
    <div className="d-flex flex-column gap-4">
      <article className="card rounded-4 p-4 d-flex flex-column gap-3" data-testid="confirmed-loan">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div>
            <h2 className="h5 text-navy mb-0">{terms.lender}</h2>
            <div className="small text-body-secondary">
              <FormattedMessage
                id="loan.summary.sources"
                values={{ document: fromDocument, declared: sources.length - fromDocument }}
              />
            </div>
          </div>
          <button type="button" className="btn btn-light btn-sm" onClick={onEdit}>
            <FormattedMessage id="loan.summary.edit" />
          </button>
        </div>

        <div className="row g-3">
          {summary.currentInterestRate !== null && (
            <Figure label="loan.summary.rate" value={percent(summary.currentInterestRate, 3)}>
              {summary.rateBasis === 'EURIBOR_PLUS_MARGIN' && terms.margin !== null && (
                <FormattedMessage
                  id="loan.summary.rateDetail"
                  values={{
                    euribor: percent(summary.euribor6m, 3),
                    margin: percent(terms.margin),
                  }}
                />
              )}
            </Figure>
          )}
          {summary.balance !== null && (
            <Figure label="loan.summary.balance" value={euro(summary.balance)}>
              <FormattedMessage id={`loan.summary.balance.${summary.balanceSource}`} />
            </Figure>
          )}
          {summary.remainingMonths !== null && (
            <Figure
              label="loan.summary.remaining"
              value={intl.formatMessage(
                { id: 'loan.summary.remainingValue' },
                {
                  years: Math.floor(summary.remainingMonths / 12),
                  months: summary.remainingMonths % 12,
                },
              )}
            >
              {summary.ageAtMaturity !== null && (
                <FormattedMessage
                  id="loan.summary.ageAtMaturity"
                  values={{ age: summary.ageAtMaturity }}
                />
              )}
            </Figure>
          )}
          {summary.loanToValue !== null && (
            <Figure
              label="loan.summary.ltv"
              value={intl.formatNumber(summary.loanToValue, { style: 'percent' })}
            >
              <FormattedMessage id="loan.summary.ltvDetail" />
            </Figure>
          )}
        </div>

        <LoanChecks terms={terms} summary={summary} />
      </article>

      {canCalculate && (
        <section className="d-flex flex-column gap-2">
          <h2 className="h5 text-navy mb-0">
            <FormattedMessage id="loan.calculator.heading" />
          </h2>
          <SavingsCalculator
            validated={{
              balance: summary.balance!,
              remainingMonths: summary.remainingMonths!,
              margin: terms.margin!,
            }}
          />
        </section>
      )}
    </div>
  )
}

function Figure({
  label,
  value,
  children,
}: {
  label: string
  value: string
  children?: ReactNode
}) {
  return (
    <div className="col-sm-6 col-lg-3">
      <div className="figure-tile h-100">
        <div className="small text-body-secondary">
          <FormattedMessage id={label} />
        </div>
        <div className="fs-5 fw-bold text-navy">{value}</div>
        {children && <div className="small text-body-secondary">{children}</div>}
      </div>
    </div>
  )
}
