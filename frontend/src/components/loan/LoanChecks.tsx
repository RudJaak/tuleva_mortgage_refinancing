import { FormattedMessage, useIntl } from 'react-intl'
import { scenarioMargins, useCalculatorSettings } from '../calculator/settings'
import type { LoanSummary, LoanTerms } from './api'

type Check = { id: string; state: 'ok' | 'attention' | 'info'; values?: Record<string, string> }

/**
 * What the pre-screening looked at and what it found, in plain words: one line per check, with the
 * borrower's own figures. The thresholds match LoanAnalyzer (10 years, €50 000, the first target).
 */
export function LoanChecks({ terms, summary }: { terms: LoanTerms; summary: LoanSummary }) {
  const intl = useIntl()
  const settings = useCalculatorSettings()
  const firstTarget = scenarioMargins(settings).worst
  const flags = new Set(summary.flags)
  const percent = (value: number) =>
    intl.formatNumber(value / 100, { style: 'percent', minimumFractionDigits: 2 })
  const euro = (amount: number) =>
    intl.formatNumber(amount, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })

  const checks: Check[] = [
    {
      id: flags.has('FIXED_RATE_PERIOD') ? 'rate.fixed' : 'rate.variable',
      state: flags.has('FIXED_RATE_PERIOD') ? 'attention' : 'ok',
    },
  ]
  if (terms.margin !== null) {
    checks.push({
      id: flags.has('MARGIN_AT_OR_BELOW_TARGET') ? 'margin.low' : 'margin.ok',
      state: flags.has('MARGIN_AT_OR_BELOW_TARGET') ? 'attention' : 'ok',
      values: { margin: percent(terms.margin), target: percent(firstTarget) },
    })
  }
  if (summary.remainingMonths !== null) {
    checks.push({
      id: flags.has('REMAINING_TERM_UNDER_MINIMUM') ? 'term.short' : 'term.ok',
      state: flags.has('REMAINING_TERM_UNDER_MINIMUM') ? 'attention' : 'ok',
      values: { years: String(Math.floor(summary.remainingMonths / 12)) },
    })
  }
  if (summary.balance !== null) {
    checks.push({
      id: flags.has('BALANCE_UNDER_MINIMUM') ? 'balance.small' : 'balance.ok',
      state: flags.has('BALANCE_UNDER_MINIMUM') ? 'attention' : 'ok',
      values: { balance: euro(summary.balance) },
    })
  }
  checks.push({
    id: `party.${summary.partyCheck}`,
    state:
      summary.partyCheck === 'MATCH'
        ? 'ok'
        : summary.partyCheck === 'MISMATCH'
          ? 'attention'
          : 'info',
  })
  checks.push({
    id: flags.has('CO_BORROWERS') ? 'borrowers.several' : 'borrowers.one',
    state: flags.has('CO_BORROWERS') ? 'info' : 'ok',
    values: { count: String(summary.borrowerCount) },
  })
  if (flags.has('NOT_SIGNED_BY_BORROWER')) {
    checks.push({ id: 'signature.missing', state: 'attention' })
  }
  checks.push({
    id: flags.has('MISSING_KEY_FIELDS') ? 'fields.missing' : 'fields.ok',
    state: flags.has('MISSING_KEY_FIELDS') ? 'attention' : 'ok',
  })

  const attention = checks.filter((check) => check.state === 'attention').length
  const marks = { ok: '✓', attention: '!', info: 'i' }
  const colours = { ok: 'text-success', attention: 'text-danger', info: 'text-body-secondary' }

  return (
    <section className="d-flex flex-column gap-2" data-testid="loan-checks">
      <h3 className="h6 text-navy mb-0">
        <FormattedMessage id="loan.checks.heading" />
      </h3>
      <p className="small mb-0">
        <FormattedMessage
          id={attention === 0 ? 'loan.checks.allPassed' : 'loan.checks.attention'}
          values={{ count: attention }}
        />
      </p>
      <ul className="list-unstyled d-flex flex-column gap-1 mb-0">
        {checks.map((check) => (
          <li
            key={check.id}
            className={`d-flex gap-2 ${colours[check.state]}`}
            data-testid="loan-check"
          >
            <span aria-hidden="true" className="fw-bold">
              {marks[check.state]}
            </span>
            <span>
              <FormattedMessage id={`loan.check.${check.id}`} values={check.values} />
            </span>
          </li>
        ))}
      </ul>
      <p className="small text-body-secondary mb-0">
        <FormattedMessage id="loan.checks.note" />
      </p>
    </section>
  )
}
