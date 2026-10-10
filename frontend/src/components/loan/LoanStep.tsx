import { useState } from 'react'
import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { Loader } from '../common/Loader'
import { DocumentUpload } from '../documents/DocumentUpload'
import { failureMessageId } from '../documents/failure'
import { ProcessingProgress } from '../documents/ProcessingProgress'
import { SealBadge } from '../documents/SealBadge'
import { paths } from '../paths'
import { useLoan, useLoanContracts, useUploadLoanContract } from './api'
import { ConfirmedLoan } from './ConfirmedLoan'
import { toFormValues } from './form'
import { LoanConfirmForm } from './LoanConfirmForm'
import { contractStages } from './stages'

export function LoanStep() {
  const loan = useLoan()
  const contracts = useLoanContracts()
  const upload = useUploadLoanContract()
  const [editing, setEditing] = useState(false)
  const [manual, setManual] = useState(false)

  const unauthorized = [loan.error, contracts.error].some(
    (error) => error instanceof ApiError && error.status === 401,
  )
  if (unauthorized) {
    return (
      <p className="alert alert-info mb-0">
        <FormattedMessage
          id="documents.loginRequired"
          values={{ link: (text) => <Link to={paths.login}>{text}</Link> }}
        />
      </p>
    )
  }
  if (loan.isPending || contracts.isPending) {
    return <Loader />
  }

  const confirmed = loan.data ?? null
  if (confirmed && !editing) {
    return <ConfirmedLoan view={confirmed} onEdit={() => setEditing(true)} />
  }

  const latest = contracts.data?.[0]
  const read = latest?.status === 'PROCESSED' ? latest.result : null
  const done = () => {
    setEditing(false)
    setManual(false)
  }

  return (
    <div className="d-flex flex-column gap-4">
      <DocumentUpload
        hintId="loan.upload.hint"
        chooseId="loan.upload.choose"
        onSelect={([file]) => upload.mutate(file)}
        pending={upload.isPending}
        error={upload.error}
      />

      {latest && (
        <article
          className="card rounded-4 p-4 d-flex flex-column gap-3"
          data-testid="contract-card"
        >
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
            <h2 className="h5 text-navy mb-0">
              <FormattedMessage id="loan.card.heading" />
            </h2>
            <SealBadge verification={latest.verification} />
          </div>
          {latest.status === 'RECEIVED' && (
            <ProcessingProgress
              createdAt={latest.createdAt}
              stages={contractStages}
              labelId="loan.progress.label"
            />
          )}
          {latest.status === 'FAILED' && (
            <p className="alert alert-danger mb-0" role="alert">
              <FormattedMessage id={failureMessageId(latest.failure?.reason)} />
            </p>
          )}
        </article>
      )}

      {read ? (
        <LoanConfirmForm
          key={latest!.documentId}
          documentId={latest!.documentId}
          initial={toFormValues(
            confirmed?.terms.documentId === latest!.documentId ? confirmed.terms : read.contract,
          )}
          read={read.contract}
          evidence={read.contract.evidence}
          estimatedBalance={
            read.summary.balanceSource === 'ESTIMATED' ? read.summary.balance : null
          }
          onConfirmed={done}
        />
      ) : manual || (confirmed && editing) ? (
        <LoanConfirmForm
          documentId={null}
          initial={toFormValues(confirmed?.terms ?? null)}
          read={null}
          evidence={{}}
          estimatedBalance={null}
          onConfirmed={done}
        />
      ) : (
        latest?.status !== 'RECEIVED' && (
          <div>
            <button type="button" className="btn btn-link px-0" onClick={() => setManual(true)}>
              <FormattedMessage id="loan.manual" />
            </button>
          </div>
        )
      )}
    </div>
  )
}
