// Adapted from TulevaEE/onboarding-client src/components/common/cancelButton (MIT, (c) 2017 Tuleva).
import { FormattedMessage } from 'react-intl'

export function CancelButton({
  onCancel,
  className = '',
}: {
  onCancel: () => void
  className?: string
}) {
  return (
    <button
      type="button"
      className={`btn btn-outline-primary btn-lg w-100 text-wrap text-balance ${className}`}
      onClick={onCancel}
    >
      <FormattedMessage id="login.stop" />
    </button>
  )
}
