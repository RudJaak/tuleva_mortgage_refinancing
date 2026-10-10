import { useEffect, useState } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { estimatedProgress, stageFor, typicalSeconds } from './progress'

/**
 * Extraction runs on the server for about half a minute and reports no progress of its own, so
 * this estimates it from the time since upload. The estimate survives leaving and returning to
 * the page because it starts from the server's `createdAt`.
 */
export function ProcessingProgress({ createdAt }: { createdAt: string }) {
  const intl = useIntl()
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 500)
    return () => clearInterval(timer)
  }, [])
  const elapsed = now - Date.parse(createdAt)
  const progress = estimatedProgress(elapsed)

  return (
    <div className="d-flex flex-column gap-2">
      <div className="d-flex justify-content-between gap-3">
        <span className="text-navy fw-medium" role="status" aria-live="polite">
          <FormattedMessage id={stageFor(elapsed)} />
        </span>
        <span className="small text-body-secondary text-nowrap">
          <FormattedMessage id="statements.progress.typical" values={{ seconds: typicalSeconds }} />
        </span>
      </div>
      <div
        className="progress statement-progress"
        role="progressbar"
        aria-valuenow={progress}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={intl.formatMessage({ id: 'statements.progress.label' })}
      >
        <div
          className="progress-bar progress-bar-striped progress-bar-animated"
          style={{ width: `${progress}%` }}
        />
      </div>
      <p className="small text-body-secondary mb-0">
        <FormattedMessage id="statements.progress.continue" />
      </p>
    </div>
  )
}
