import type { ReactNode } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import type { StatementSummary } from './api'

export function StatementSummaryView({ summary }: { summary: StatementSummary }) {
  const intl = useIntl()
  const euros = (amount: number) =>
    intl.formatNumber(amount, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })
  const perMonth = (amount: number) =>
    intl.formatMessage({ id: 'statements.perMonth' }, { amount: euros(amount) })
  const category = (id: string, creditor: string | null) =>
    creditor ?? intl.formatMessage({ id: `statements.category.${id}` })

  return (
    <div className="d-flex flex-column gap-4">
      <div className="row g-3">
        <Figure
          label="statements.figure.income"
          value={perMonth(summary.income.averageMonthlyTotal)}
        >
          <FormattedMessage
            id="statements.figure.incomeDetail"
            values={{
              months: summary.income.monthsWithSalary,
              lowest: euros(summary.income.lowestMonthlySalary),
            }}
          />
        </Figure>
        <Figure label="statements.figure.obligations" value={perMonth(summary.monthlyObligations)}>
          {summary.debtServiceRatio !== null && (
            <FormattedMessage
              id="statements.figure.debtServiceRatio"
              values={{
                ratio: intl.formatNumber(summary.debtServiceRatio, { style: 'percent' }),
              }}
            />
          )}
        </Figure>
        <Figure label="statements.figure.card" value={perMonth(summary.monthlyCardRepayments)}>
          <FormattedMessage id="statements.figure.cardDetail" />
        </Figure>
        <Figure label="statements.figure.savings" value={perMonth(summary.monthlySavings)} />
      </div>

      {summary.obligations.length > 0 && (
        <section>
          <h3 className="h6 text-navy">
            <FormattedMessage id="statements.obligations.heading" />
          </h3>
          <ul className="list-unstyled mb-0">
            {summary.obligations.map((obligation) => (
              <li
                key={`${obligation.category}-${obligation.creditor}`}
                className="d-flex justify-content-between gap-3 py-1 border-bottom"
              >
                <span>
                  {category(obligation.category, obligation.creditor)}{' '}
                  <span className="text-body-secondary small">
                    <FormattedMessage id={`statements.category.${obligation.category}`} />
                  </span>
                </span>
                <span className="text-nowrap">{perMonth(obligation.typicalMonthly)}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {summary.oneOffPayments.length > 0 && (
        <section>
          <h3 className="h6 text-navy">
            <FormattedMessage id="statements.oneOffs.heading" />
          </h3>
          <ul className="list-unstyled small mb-0">
            {summary.oneOffPayments.map((payment) => (
              <li key={`${payment.date}-${payment.amount}`}>
                <FormattedMessage
                  id="statements.oneOffs.item"
                  values={{
                    date: intl.formatDate(payment.date, { dateStyle: 'short' }),
                    creditor: category(payment.category, payment.creditor),
                    amount: euros(payment.amount),
                  }}
                />
              </li>
            ))}
          </ul>
        </section>
      )}

      <ul className="list-unstyled d-flex flex-column gap-1 mb-0">
        {summary.holderCheck === 'MATCH' && (
          <li className="text-success">
            <span aria-hidden="true">✓ </span>
            <FormattedMessage id="statements.holder.MATCH" />
          </li>
        )}
        {summary.flags.length === 0 ? (
          <li className="text-success">
            <span aria-hidden="true">✓ </span>
            <FormattedMessage id="statements.flags.none" />
          </li>
        ) : (
          summary.flags.map((flag) => (
            <li key={flag.code} className="text-danger" data-testid="statement-flag">
              <span aria-hidden="true">! </span>
              <FormattedMessage
                id={`statements.flag.${flag.code}`}
                values={{ count: flag.occurrences }}
              />
            </li>
          ))
        )}
      </ul>
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
      <div className="statement-figure h-100">
        <div className="small text-body-secondary">
          <FormattedMessage id={label} />
        </div>
        <div className="fs-5 fw-bold text-navy">{value}</div>
        {children && <div className="small text-body-secondary">{children}</div>}
      </div>
    </div>
  )
}
