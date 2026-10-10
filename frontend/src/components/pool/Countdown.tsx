import { useEffect, useState } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { remainingUntil } from './remaining'

function digits(value: number) {
  return String(value).padStart(2, '0').split('')
}

export function Countdown({ deadline }: { deadline: string }) {
  const intl = useIntl()
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 15_000)
    return () => window.clearInterval(timer)
  }, [])
  const remaining = remainingUntil(new Date(deadline), now)
  const blocks = [
    { value: remaining.days, labelId: 'tracker.countdown.days' },
    { value: remaining.hours, labelId: 'tracker.countdown.hours' },
    { value: remaining.minutes, labelId: 'tracker.countdown.minutes' },
  ]
  return (
    <div
      className="counter-small d-flex"
      role="timer"
      aria-label={intl.formatMessage({ id: 'tracker.countdown.label' }, remaining)}
    >
      {blocks.map((block) => (
        <div key={block.labelId} className="counter-block" aria-hidden="true">
          <div className="counter-number">
            {digits(block.value).map((digit, index) => (
              <span key={index}>{digit}</span>
            ))}
          </div>
          <div className="counter-label mt-2">
            <FormattedMessage id={block.labelId} />
          </div>
        </div>
      ))}
    </div>
  )
}
