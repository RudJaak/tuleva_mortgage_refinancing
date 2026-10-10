import { useIntl } from 'react-intl'
import { IconBeforeLabel, QrCodeIcon } from './icons'

export function SmartIdTab({ onStart }: { onStart: () => void }) {
  const { formatMessage } = useIntl()
  return (
    <div className="d-grid gap-3">
      <button
        type="button"
        className="btn btn-primary btn-lg text-wrap text-balance"
        onClick={onStart}
      >
        <IconBeforeLabel icon={<QrCodeIcon />} label={formatMessage({ id: 'login.enter' })} />
      </button>
    </div>
  )
}
