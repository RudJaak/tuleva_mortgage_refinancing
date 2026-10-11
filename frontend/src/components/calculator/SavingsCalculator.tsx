import { useId, useState } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { compareMargins } from '../../calc/savings'
import { useMembership } from '../membership/api'
import { MarginSlider } from './MarginSlider'
import { minimumGoalMargin, useCalculatorSettings } from './settings'

export type ValidatedLoan = {
  balance: number
  remainingMonths: number
  margin: number
}

type CalculatorInputs = {
  balance: string
  years: string
  months: string
  // null until the borrower moves the slider: the configured default margin applies
  margin: number | null
}

const initialInputs: CalculatorInputs = {
  balance: '180000',
  years: '20',
  months: '6',
  margin: null,
}

function toNumber(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0
}

export function SavingsCalculator({ validated }: { validated?: ValidatedLoan }) {
  const intl = useIntl()
  const id = useId()
  const settings = useCalculatorSettings()
  const [inputs, setInputs] = useState(initialInputs)
  // Members have already paid the fee; for anyone else (or not yet known) it is a cost of joining
  const member = useMembership().data?.member === true
  const membershipCost = member ? 0 : settings.membershipFee

  const balance = validated?.balance ?? toNumber(inputs.balance)
  const remainingMonths =
    validated?.remainingMonths ??
    Math.round(toNumber(inputs.years) * 12) + Math.min(Math.round(toNumber(inputs.months)), 11)
  const currentMargin = validated?.margin ?? inputs.margin ?? settings.defaultMargin
  const targetMargin = minimumGoalMargin(settings)

  const result = compareMargins({
    balance,
    remainingMonths,
    euribor: settings.euribor6m,
    currentMargin,
    targetMargin,
    switchingCost: settings.switchingCost + membershipCost,
    annualDiscountRate: settings.annualDiscountRate,
  })

  const euro = (amount: number) =>
    intl.formatNumber(amount, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })
  const percent = (value: number) =>
    intl.formatNumber(value / 100, {
      style: 'percent',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })
  const update = (patch: Partial<CalculatorInputs>) => setInputs((prev) => ({ ...prev, ...patch }))

  return (
    <div className="card calculator rounded-4">
      <div className="card-body p-2">
        <div className="bg-blue-2 p-3 rounded-3">
          {validated ? (
            <ValidatedRows
              rows={[
                ['calculator.balance', euro(validated.balance)],
                [
                  'calculator.years',
                  intl.formatMessage(
                    { id: 'calculator.term' },
                    {
                      years: Math.floor(validated.remainingMonths / 12),
                      months: validated.remainingMonths % 12,
                    },
                  ),
                ],
                ['calculator.margin', percent(validated.margin)],
              ]}
            />
          ) : (
            <>
              <div className="mb-3 align-items-center row">
                <label htmlFor={`${id}-balance`} className="col-sm-6 col-form-label pe-0">
                  <FormattedMessage id="calculator.balance" />
                </label>
                <div className="col-sm-6">
                  <div className="input-group">
                    <input
                      id={`${id}-balance`}
                      type="number"
                      inputMode="numeric"
                      className="form-control text-end"
                      min={0}
                      max={2_000_000}
                      step={1000}
                      placeholder="180000"
                      value={inputs.balance}
                      onChange={(event) => update({ balance: event.target.value })}
                    />
                    <span className="input-group-text calculator-unit">€</span>
                  </div>
                </div>
              </div>
              <div className="mb-3 align-items-center row">
                <label htmlFor={`${id}-years`} className="col-sm-6 col-form-label pe-0">
                  <FormattedMessage id="calculator.years" />
                </label>
                <div className="col-sm-6 d-flex gap-2">
                  <div className="input-group">
                    <input
                      id={`${id}-years`}
                      type="number"
                      inputMode="numeric"
                      className="form-control text-end"
                      min={0}
                      max={35}
                      step={1}
                      placeholder="20"
                      value={inputs.years}
                      onChange={(event) => update({ years: event.target.value })}
                    />
                    <span className="input-group-text">
                      <FormattedMessage id="calculator.yearsShort" />
                    </span>
                  </div>
                  <div className="input-group">
                    <input
                      id={`${id}-months`}
                      type="number"
                      inputMode="numeric"
                      className="form-control text-end"
                      min={0}
                      max={11}
                      step={1}
                      placeholder="0"
                      aria-label={intl.formatMessage({ id: 'calculator.months' })}
                      value={inputs.months}
                      onChange={(event) => update({ months: event.target.value })}
                    />
                    <span className="input-group-text">
                      <FormattedMessage id="calculator.monthsShort" />
                    </span>
                  </div>
                </div>
              </div>
              <div className="mb-0 row">
                <label htmlFor={`${id}-margin`} className="col-sm-6 col-form-label pe-0">
                  <FormattedMessage id="calculator.margin" />
                </label>
                <div className="col-sm-6">
                  <MarginSlider
                    id={`${id}-margin`}
                    value={currentMargin}
                    marker={settings.marketAverageMargin}
                    markerLabel={intl.formatMessage(
                      { id: 'calculator.marketAverage' },
                      {
                        margin: intl.formatNumber(settings.marketAverageMargin / 100, {
                          style: 'percent',
                          maximumFractionDigits: 2,
                        }),
                      },
                    )}
                    format={percent}
                    onChange={(margin) => update({ margin })}
                  />
                </div>
              </div>
            </>
          )}
        </div>

        <p className="mt-3 mb-0 px-3 small text-navy" data-testid="goal-note">
          <FormattedMessage
            id="calculator.goalNote"
            values={{ margin: percent(targetMargin), strong: (text) => <strong>{text}</strong> }}
          />
        </p>

        <div className="mt-3 px-3 text-navy" aria-live="polite">
          <ResultRow
            labelId="calculator.current"
            amount={intl.formatMessage(
              { id: 'calculator.perMonth' },
              { amount: euro(result.currentPayment) },
            )}
            detail={intl.formatMessage(
              { id: 'calculator.rateBreakdown' },
              { margin: percent(currentMargin), euribor: percent(settings.euribor6m) },
            )}
          />
          <ResultRow
            labelId="calculator.target"
            amount={intl.formatMessage(
              { id: 'calculator.perMonth' },
              { amount: euro(result.targetPayment) },
            )}
            detail={intl.formatMessage(
              { id: 'calculator.rateBreakdown' },
              { margin: percent(targetMargin), euribor: percent(settings.euribor6m) },
            )}
          />
        </div>
        <hr className="my-3" />
        <div className="px-3 mb-2 text-navy">
          {result.hasSaving ? (
            <>
              {/* The whole-loan saving is the point; the monthly figure only puts it in scale */}
              <div className="d-flex flex-wrap align-items-baseline justify-content-between column-gap-3">
                <span className="fw-medium">
                  <FormattedMessage id="calculator.saving" />
                </span>
                <span
                  className="calculator-total lh-1 fw-bold text-success text-nowrap"
                  data-testid="lifetime-saving"
                >
                  {euro(result.lifetimeSaving)}
                </span>
              </div>
              <div className="text-end text-success fw-medium mt-1" data-testid="monthly-saving">
                <FormattedMessage
                  id="calculator.monthlySaving"
                  values={{ amount: euro(result.monthlySaving) }}
                />
              </div>
              <ul className="small text-body-secondary list-unstyled mt-2 mb-1" data-testid="costs">
                <li>
                  <FormattedMessage
                    id="calculator.cost.switching"
                    values={{ cost: euro(settings.switchingCost) }}
                  />
                </li>
                <li data-testid="membership-fee">
                  {member ? (
                    <FormattedMessage
                      id="calculator.cost.membershipPaid"
                      values={{
                        fee: euro(settings.membershipFee),
                        struck: (text) => <s>{text}</s>,
                      }}
                    />
                  ) : (
                    <FormattedMessage
                      id="calculator.cost.membership"
                      values={{ fee: euro(settings.membershipFee) }}
                    />
                  )}
                </li>
              </ul>
              <p className="small text-body-secondary mb-0">
                <FormattedMessage
                  id="calculator.savingDetails"
                  values={{
                    npv: euro(result.netPresentValue),
                    months: Math.ceil(result.breakEvenMonths ?? 0),
                  }}
                />
              </p>
            </>
          ) : (
            <p className="mb-0 text-body-secondary">
              <FormattedMessage id="calculator.noSaving" />
            </p>
          )}
          <p className="small text-body-tertiary mt-3 mb-0">
            <FormattedMessage
              id="calculator.disclaimer"
              values={{
                euribor: percent(settings.euribor6m),
                date: intl.formatDate(settings.euriborDate, { dateStyle: 'short' }),
              }}
            />
          </p>
        </div>
      </div>
    </div>
  )
}

function ValidatedRows({ rows }: { rows: [string, string][] }) {
  return (
    <dl className="mb-0 d-flex flex-column gap-2">
      {rows.map(([labelId, value]) => (
        <div key={labelId} className="d-flex align-items-center justify-content-between gap-3">
          <dt className="fw-normal">
            <FormattedMessage id={labelId} />
          </dt>
          <dd className="mb-0 d-flex align-items-center gap-2">
            <span className="fw-medium text-navy text-nowrap">{value}</span>
            <span className="badge rounded-pill text-success bg-success-subtle">
              <FormattedMessage id="calculator.validated" />
            </span>
          </dd>
        </div>
      ))}
    </dl>
  )
}

function ResultRow({
  labelId,
  amount,
  detail,
}: {
  labelId: string
  amount: string
  detail: string
}) {
  return (
    <div className="mb-2">
      <div className="d-flex align-items-baseline justify-content-between gap-3">
        <span>
          <FormattedMessage id={labelId} />
        </span>
        <span className="fs-5 lh-1 fw-bold text-nowrap">{amount}</span>
      </div>
      <div className="small text-body-secondary">{detail}</div>
    </div>
  )
}
