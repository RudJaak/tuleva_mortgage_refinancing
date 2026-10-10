import { FormattedMessage, useIntl } from 'react-intl'
import { usePoolStats, type MilestoneStatus, type PoolStats } from './api'
import { Countdown } from './Countdown'

export function PoolTracker() {
  const { data, isError } = usePoolStats()
  return (
    <section className="pool-tracker card rounded-4 p-4" aria-labelledby="pool-tracker-heading">
      <h2 id="pool-tracker-heading" className="text-navy mb-4">
        <FormattedMessage id="tracker.heading" />
      </h2>
      {data ? (
        <TrackerContent stats={data} />
      ) : (
        <p className="text-body-secondary mb-0">
          <FormattedMessage id={isError ? 'tracker.unavailable' : 'tracker.loading'} />
        </p>
      )}
    </section>
  )
}

function TrackerContent({ stats }: { stats: PoolStats }) {
  const intl = useIntl()
  const millions = (volume: number) =>
    intl.formatMessage(
      { id: 'tracker.millions' },
      { amount: intl.formatNumber(volume / 1_000_000, { maximumFractionDigits: 1 }) },
    )
  const percent = (margin: number) =>
    intl.formatNumber(margin / 100, { style: 'percent', minimumFractionDigits: 2 })
  const visible = stats.joinedCount !== null && stats.committedVolume !== null
  const volume = stats.committedVolume ?? 0
  const next = stats.nextMilestone
  const progress = next ? Math.min(100, Math.floor((volume / next.volume) * 100)) : 100

  return (
    <div className="d-flex flex-column gap-4">
      <div className="row gy-4 align-items-end">
        <div className="col-sm-4">
          <div className="tracker-figure" data-testid="joined-count">
            {visible ? intl.formatNumber(stats.joinedCount!) : '—'}
          </div>
          <div className="text-body-secondary">
            <FormattedMessage id="tracker.joined" />
          </div>
        </div>
        <div className="col-sm-4">
          <div className="tracker-figure" data-testid="committed-volume">
            {visible ? millions(volume) : '—'}
          </div>
          <div className="text-body-secondary">
            <FormattedMessage id="tracker.volume" />
          </div>
        </div>
        <div className="col-sm-4">
          <Countdown deadline={stats.deadline} />
          <div className="text-body-secondary mt-1">
            <FormattedMessage id="tracker.untilDeadline" />
          </div>
        </div>
      </div>

      {!visible && (
        <p className="mb-0 text-body-secondary">
          <FormattedMessage id="tracker.hidden" />
        </p>
      )}

      {visible && next && (
        <div>
          <div className="d-flex justify-content-between gap-3 mb-2">
            <span className="text-navy fw-medium">
              <FormattedMessage
                id={stats.minimumReached ? 'tracker.nextMilestone' : 'tracker.minimumMilestone'}
                values={{ volume: millions(next.volume), margin: percent(next.margin) }}
              />
            </span>
            <span className="text-navy fw-bold">{progress}%</span>
          </div>
          <div
            className="progress"
            role="progressbar"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={intl.formatMessage({ id: 'tracker.progressLabel' })}
          >
            <div className="progress-bar" style={{ width: `${progress}%` }} />
          </div>
          <p className="small text-body-secondary mt-2 mb-0">
            <FormattedMessage
              id="tracker.remaining"
              values={{ amount: millions(Math.max(0, next.volume - volume)) }}
            />
          </p>
        </div>
      )}

      <ol className="row g-3 list-unstyled mb-0">
        {stats.milestones.map((milestone, index) => (
          <li key={milestone.volume} className="col-sm-4">
            <MilestoneCard
              milestone={milestone}
              isMinimum={index === 0}
              isNext={milestone.volume === next?.volume}
              millions={millions}
              percent={percent}
            />
          </li>
        ))}
      </ol>

      <p className="small text-body-tertiary mb-0">
        <FormattedMessage
          id="tracker.disclaimer"
          values={{
            minimum: millions(stats.minimumVolume),
            date: intl.formatDate(stats.deadline, {
              dateStyle: 'short',
              timeZone: 'Europe/Tallinn',
            }),
          }}
        />
      </p>
    </div>
  )
}

function MilestoneCard({
  milestone,
  isMinimum,
  isNext,
  millions,
  percent,
}: {
  milestone: MilestoneStatus
  isMinimum: boolean
  isNext: boolean
  millions: (volume: number) => string
  percent: (margin: number) => string
}) {
  const state = milestone.reached ? 'milestone-reached' : isNext ? 'milestone-next' : ''
  return (
    <div className={`milestone ${state}`} data-testid="milestone">
      <div className="d-flex justify-content-between align-items-baseline gap-2">
        <span className="fw-bold text-navy">{millions(milestone.volume)}</span>
        {milestone.reached && (
          <span className="small text-success fw-medium">
            <FormattedMessage id="tracker.milestone.reached" />
          </span>
        )}
      </div>
      <div className="text-navy">
        <FormattedMessage
          id="tracker.milestone.margin"
          values={{ margin: <strong>{percent(milestone.margin)}</strong> }}
        />
      </div>
      {isMinimum && (
        <div className="small text-body-secondary">
          <FormattedMessage id="tracker.milestone.minimum" />
        </div>
      )}
    </div>
  )
}
