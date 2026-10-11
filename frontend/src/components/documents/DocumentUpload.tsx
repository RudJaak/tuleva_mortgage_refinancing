import { useRef, useState, type DragEvent } from 'react'
import { FormattedMessage } from 'react-intl'
import { ApiError } from '../../api/client'
import { failureMessageId } from './failure'

/** A file picker that also takes files dropped on it; {@code multiple} allows several at once. */
export function DocumentUpload({
  headingId,
  badgeId,
  hintId,
  chooseId,
  dropId,
  accept = '.asice,.bdoc,.pdf,application/pdf',
  className = '',
  multiple = false,
  onSelect,
  pending,
  error,
}: {
  headingId?: string
  badgeId?: string
  hintId: string
  chooseId: string
  dropId?: string
  accept?: string
  className?: string
  multiple?: boolean
  onSelect: (files: File[]) => void
  pending: boolean
  error: Error | null
}) {
  const input = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const messageId =
    error === null
      ? undefined
      : error instanceof ApiError && error.status === 401
        ? 'documents.failure.UNAUTHORIZED'
        : failureMessageId(error instanceof ApiError ? error.reason : undefined)

  const select = (files: FileList | null) => {
    const chosen = Array.from(files ?? [])
    if (chosen.length > 0 && !pending) {
      onSelect(multiple ? chosen : chosen.slice(0, 1))
    }
  }
  const onDrop = (event: DragEvent) => {
    event.preventDefault()
    setDragging(false)
    select(event.dataTransfer.files)
  }

  return (
    <div
      className={`card rounded-4 p-4 d-flex flex-column gap-3 document-drop ${dragging ? 'document-drop-active' : ''} ${className}`}
      data-testid="document-drop"
      onDragOver={(event) => {
        event.preventDefault()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
    >
      {headingId && (
        <h2 className="h6 text-navy mb-0 d-flex flex-wrap align-items-center gap-2">
          <FormattedMessage id={headingId} />
          {badgeId && (
            <span className="badge rounded-pill text-success bg-success-subtle">
              <FormattedMessage id={badgeId} />
            </span>
          )}
        </h2>
      )}
      <p className="mb-0">
        <FormattedMessage id={hintId} />
      </p>
      <div className="d-flex flex-wrap align-items-center gap-3 mt-auto">
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
            accept={accept}
            multiple={multiple}
            disabled={pending}
            onChange={(event) => {
              select(event.target.files)
              if (input.current) {
                input.current.value = ''
              }
            }}
          />
        </label>
        <span className="small text-body-secondary">
          <FormattedMessage
            id={dropId ?? (multiple ? 'documents.upload.dropMany' : 'documents.upload.drop')}
          />
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
