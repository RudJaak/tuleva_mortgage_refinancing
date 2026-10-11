import { useState } from 'react'
import { FormattedMessage } from 'react-intl'
import { DocumentUpload } from '../documents/DocumentUpload'

/**
 * The three ways to give the loan's terms, side by side: a screenshot of the internet bank's loan
 * page (not read yet), the contract itself, or typing the figures in.
 */
export function LoanSourceOptions({
  onContract,
  pending,
  error,
  manual,
  onManual,
}: {
  onContract: (file: File) => void
  pending: boolean
  error: Error | null
  manual: boolean
  onManual: () => void
}) {
  const [screenshot, setScreenshot] = useState(false)

  return (
    <div className="d-flex flex-column gap-3">
      <div className="row g-3">
        <div className="col-lg-4">
          <DocumentUpload
            className="h-100 loan-source"
            headingId="loan.source.screenshot.heading"
            hintId="loan.source.screenshot.hint"
            chooseId="loan.source.screenshot.choose"
            dropId="loan.source.screenshot.drop"
            accept="image/png,image/jpeg,image/webp,image/heic"
            onSelect={() => setScreenshot(true)}
            pending={false}
            error={null}
          />
        </div>
        <div className="col-lg-4">
          <DocumentUpload
            className="h-100 loan-source"
            headingId="loan.source.contract.heading"
            badgeId="loan.source.contract.badge"
            hintId="loan.source.contract.hint"
            chooseId="loan.upload.choose"
            onSelect={([file]) => onContract(file)}
            pending={pending}
            error={error}
          />
        </div>
        <div className="col-lg-4">
          <div
            className={`card rounded-4 p-4 h-100 d-flex flex-column gap-3 loan-source ${manual ? 'loan-source-active' : ''}`}
          >
            <h2 className="h6 text-navy mb-0">
              <FormattedMessage id="loan.source.manual.heading" />
            </h2>
            <p className="mb-0">
              <FormattedMessage id="loan.source.manual.hint" />
            </p>
            <div className="mt-auto">
              <button
                type="button"
                className="btn btn-outline-primary"
                aria-pressed={manual}
                onClick={onManual}
              >
                <FormattedMessage id="loan.source.manual.choose" />
              </button>
            </div>
          </div>
        </div>
      </div>
      {screenshot && (
        <p className="alert alert-info mb-0" role="status">
          <FormattedMessage id="loan.source.screenshot.soon" />
        </p>
      )}
    </div>
  )
}
