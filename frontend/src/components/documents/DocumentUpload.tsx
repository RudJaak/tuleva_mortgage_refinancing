import { useRef } from 'react'
import { FormattedMessage } from 'react-intl'
import { ApiError } from '../../api/client'
import { failureMessageId } from './failure'

export function DocumentUpload({
  hintId,
  chooseId,
  onSelect,
  pending,
  error,
}: {
  hintId: string
  chooseId: string
  onSelect: (file: File) => void
  pending: boolean
  error: Error | null
}) {
  const input = useRef<HTMLInputElement>(null)
  const messageId =
    error === null
      ? undefined
      : error instanceof ApiError && error.status === 401
        ? 'documents.failure.UNAUTHORIZED'
        : failureMessageId(error instanceof ApiError ? error.reason : undefined)

  return (
    <div className="card rounded-4 p-4 d-flex flex-column gap-3">
      <p className="mb-0">
        <FormattedMessage id={hintId} />
      </p>
      <div className="d-flex flex-wrap align-items-center gap-3">
        <label className={`btn btn-primary mb-0 ${pending ? 'disabled' : ''}`}>
          {pending ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
              <FormattedMessage id="documents.upload.checking" />
            </>
          ) : (
            <FormattedMessage id={chooseId} />
          )}
          <input
            ref={input}
            type="file"
            className="visually-hidden"
            accept=".asice,.bdoc,.pdf,application/pdf"
            disabled={pending}
            onChange={(event) => {
              const file = event.target.files?.[0]
              if (file) {
                onSelect(file)
              }
              if (input.current) {
                input.current.value = ''
              }
            }}
          />
        </label>
        <span className="small text-body-secondary">
          <FormattedMessage id="documents.upload.formats" />
        </span>
      </div>
      {messageId && (
        <p className="alert alert-danger mb-0" role="alert">
          <FormattedMessage id={messageId} />
        </p>
      )}
    </div>
  )
}
