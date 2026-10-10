import { useForm, useWatch } from 'react-hook-form'
import { FormattedMessage, useIntl } from 'react-intl'
import { useConfirmLoan, type Evidence, type ExtractedContract, type LoanFieldKey } from './api'
import { confirmableFields, isNumber, toFormValues, toTerms, type FormValues } from './form'

const rateTypes = ['VARIABLE', 'MIXED', 'FIXED'] as const
const repaymentTypes = ['ANNUITY', 'EQUAL_PRINCIPAL'] as const

/**
 * Every field read from the contract, pre-filled, with the page and quote it came from. The
 * borrower confirms or corrects each one; the backend records which values they changed.
 */
export function LoanConfirmForm({
  documentId,
  initial,
  read,
  evidence,
  estimatedBalance,
  onConfirmed,
}: {
  documentId: string | null
  initial: FormValues
  read: ExtractedContract | null
  evidence: Partial<Record<LoanFieldKey, Evidence>>
  estimatedBalance: number | null
  onConfirmed: () => void
}) {
  const intl = useIntl()
  const confirm = useConfirmLoan()
  const readValues = toFormValues(read)
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormValues>({ defaultValues: initial })
  const current = useWatch({ control })

  const onSubmit = handleSubmit((values) =>
    confirm.mutate(toTerms(values, documentId), { onSuccess: onConfirmed }),
  )

  return (
    <form className="card rounded-4 p-4 d-flex flex-column gap-3" onSubmit={onSubmit} noValidate>
      <div>
        <h2 className="h5 text-navy mb-1">
          <FormattedMessage id="loan.confirm.heading" />
        </h2>
        <p className="small text-body-secondary mb-0">
          <FormattedMessage id={read ? 'loan.confirm.lead' : 'loan.confirm.manualLead'} />
        </p>
      </div>

      {confirmableFields
        .filter((field) => field.key !== 'FIXED_RATE_UNTIL' || current.rateType !== 'VARIABLE')
        .map((field) => {
          const id = `loan-${field.name}`
          const source = evidence[field.key]
          const changed = read !== null && current[field.name] !== readValues[field.name]
          const error = errors[field.name]
          return (
            <div key={field.key} className={source?.confidence === 'LOW' ? 'low-confidence' : ''}>
              <div className="d-flex justify-content-between gap-2">
                <label htmlFor={id} className="form-label fw-medium mb-1">
                  <FormattedMessage id={`loan.field.${field.key}`} />
                </label>
                {changed && (
                  <span className="small text-body-secondary">
                    <FormattedMessage id="loan.confirm.changed" />
                  </span>
                )}
              </div>
              {field.kind === 'rateType' || field.kind === 'repaymentType' ? (
                <select id={id} className="form-select" {...register(field.name)}>
                  {(field.kind === 'rateType' ? rateTypes : repaymentTypes).map((option) => (
                    <option key={option} value={option}>
                      {intl.formatMessage({ id: `loan.${field.kind}.${option}` })}
                    </option>
                  ))}
                </select>
              ) : (
                <div className="input-group">
                  <input
                    id={id}
                    className={`form-control ${error ? 'is-invalid' : ''}`}
                    type={field.kind === 'date' ? 'date' : 'text'}
                    inputMode={
                      field.kind === 'euro' || field.kind === 'percent' ? 'decimal' : undefined
                    }
                    aria-invalid={error ? true : undefined}
                    {...register(field.name, {
                      required: 'required' in field && field.required,
                      validate: (value) =>
                        field.kind === 'euro' || field.kind === 'percent'
                          ? isNumber(String(value))
                          : true,
                    })}
                  />
                  {field.kind === 'euro' && <span className="input-group-text">€</span>}
                  {field.kind === 'percent' && <span className="input-group-text">%</span>}
                </div>
              )}
              {error && (
                <div className="small text-danger mt-1" role="alert">
                  <FormattedMessage
                    id={error.type === 'required' ? 'loan.confirm.required' : 'loan.confirm.number'}
                  />
                </div>
              )}
              {field.key === 'BALANCE' && (
                <div className="small text-body-secondary mt-1">
                  <FormattedMessage
                    id={
                      estimatedBalance === null
                        ? 'loan.confirm.balanceHint'
                        : 'loan.confirm.balanceHintEstimate'
                    }
                    values={{
                      estimate:
                        estimatedBalance === null
                          ? ''
                          : intl.formatNumber(estimatedBalance, {
                              style: 'currency',
                              currency: 'EUR',
                              maximumFractionDigits: 0,
                            }),
                    }}
                  />
                </div>
              )}
              {source && <EvidenceLine evidence={source} />}
            </div>
          )
        })}

      {confirm.isError && (
        <p className="alert alert-danger mb-0" role="alert">
          <FormattedMessage id="documents.failure.PROCESSING_FAILED" />
        </p>
      )}
      <div>
        <button type="submit" className="btn btn-primary" disabled={confirm.isPending}>
          <FormattedMessage id="loan.confirm.submit" />
        </button>
      </div>
    </form>
  )
}

function EvidenceLine({ evidence }: { evidence: Evidence }) {
  return (
    <div className="small text-body-secondary mt-1" data-testid="evidence">
      <FormattedMessage
        id="loan.evidence"
        values={{
          document: evidence.document.replace(/\.pdf$/i, ''),
          page: evidence.page,
          quote: evidence.quote,
        }}
      />
      {evidence.confidence === 'LOW' && (
        <span className="badge text-bg-warning ms-2">
          <FormattedMessage id="loan.evidence.check" />
        </span>
      )}
    </div>
  )
}
