import { useRef } from 'react'
import { FormattedMessage } from 'react-intl'
import { ApiError } from '../../api/client'
import { failureMessageId } from './failure'

export function StatementUpload({
  onSelect,
  pending,
  error,
}: {
  onSelect: (file: File) => void
  pending: boolean
  error: Error | null
}) {
  const input = useRef<HTMLInputElement>(null)
  const messageId =
    error === null
      ? undefined
      : error instanceof ApiError && error.status === 401
        ? 'statements.failure.UNAUTHORIZED'
        : failureMessageId(error instanceof ApiError ? error.reason : undefined)

  return (
    <div className="card rounded-4 p-4 d-flex flex-column gap-3 statement-upload">
      <p className="mb-0">
        <FormattedMessage id="statements.upload.hint" />
      </p>
      <div className="d-flex flex-wrap align-items-center gap-3">
        <label className={`btn btn-primary mb-0 ${pending ? 'disabled' : ''}`}>
          {pending ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
              <FormattedMessage id="statements.upload.checking" />
            </>
          ) : (
            <FormattedMessage id="statements.upload.choose" />
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
          <FormattedMessage id="statements.upload.formats" />
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
