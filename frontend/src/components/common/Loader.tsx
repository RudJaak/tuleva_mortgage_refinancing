// Adapted from TulevaEE/onboarding-client src/components/common/loader (MIT, (c) 2017 Tuleva).
import { useIntl } from 'react-intl'

export function Loader({ className = '', label }: { className?: string; label?: string }) {
  const { formatMessage } = useIntl()
  return (
    <div
      className={`loader ${className}`}
      role="status"
      aria-label={label ?? formatMessage({ id: 'common.loading' })}
    >
      <svg className="circular" viewBox="25 25 50 50">
        <circle
          className="path"
          cx="50"
          cy="50"
          r="20"
          fill="none"
          strokeWidth="3"
          strokeMiterlimit="10"
        />
      </svg>
    </div>
  )
}
