import { useState, type ReactNode } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { useAnimatedNumber } from '../common/useAnimatedNumber'
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

/**
 * The pool counted in people (D31), so every borrower feels welcome whatever their balance; the
 * euro total stays out of the counter on purpose.
 */
function TrackerContent({ stats }: { stats: PoolStats }) {
  const intl = useIntl()
  const people = (count: number) => intl.formatMessage({ id: 'tracker.people' }, { count })
  const percent = (margin: number) =>
    intl.formatNumber(margin / 100, { style: 'percent', minimumFractionDigits: 2 })
  const visible = stats.joinedCount !== null
  const exactJoined = useAnimatedNumber(stats.joinedCount ?? 0)
  const joined = Math.round(exactJoined)
  const next = stats.nextMilestone
  const exactProgress = next ? Math.min(100, (exactJoined / next.members) * 100) : 100
  const progress = Math.floor(exactProgress)

  return (
    <div className="d-flex flex-column gap-4">
      <div className="row gy-4 align-items-end">
        <div className="col-sm-6">
          <TrackerFigure testId="joined-count" value={stats.joinedCount}>
            {visible ? intl.formatNumber(joined) : '—'}
          </TrackerFigure>
          <div className="text-body-secondary">
            <FormattedMessage id="tracker.joined" />
          </div>
        </div>
        <div className="col-sm-6">
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
                values={{ people: people(next.members), margin: percent(next.margin) }}
              />
            </span>
            <span className="tracker-percent text-navy fw-bold">{progress}%</span>
          </div>
          <div
            className="progress"
            role="progressbar"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={intl.formatMessage({ id: 'tracker.progressLabel' })}
          >
            <div className="progress-bar" style={{ width: `${exactProgress}%` }} />
          </div>
          <p className="small text-body-secondary mt-2 mb-0">
            <FormattedMessage
              id="tracker.remaining"
              values={{ people: people(Math.max(0, next.members - joined)) }}
            />
          </p>
        </div>
      )}

      <ol className="row g-3 list-unstyled mb-0">
        {stats.milestones.map((milestone, index) => (
          <li key={milestone.members} className="col-sm-4">
            <MilestoneCard
              milestone={milestone}
              isMinimum={index === 0}
              isNext={milestone.members === next?.members}
              people={people}
              percent={percent}
            />
          </li>
        ))}
      </ol>

      <p className="small text-body-tertiary mb-0">
        <FormattedMessage
          id="tracker.disclaimer"
          values={{
            minimum: people(stats.minimumMembers),
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

/** Pulses its content when `value` changes after the first render. */
function TrackerFigure({
  testId,
  value,
  children,
}: {
  testId: string
  value: number | null
  children: ReactNode
}) {
  const [initial] = useState(value)
  const changed = value !== initial
  return (
    <div className="tracker-figure" data-testid={testId}>
      <span key={changed ? value : 'initial'} className={changed ? 'tracker-pulse' : undefined}>
        {children}
      </span>
    </div>
  )
}

function MilestoneCard({
  milestone,
  isMinimum,
  isNext,
  people,
  percent,
}: {
  milestone: MilestoneStatus
  isMinimum: boolean
  isNext: boolean
  people: (count: number) => string
  percent: (margin: number) => string
}) {
  const state = milestone.reached ? 'milestone-reached' : isNext ? 'milestone-next' : ''
  return (
    <div className={`milestone ${state}`} data-testid="milestone">
      <div className="d-flex justify-content-between align-items-baseline gap-2">
        <span className="fw-bold text-navy">{people(milestone.members)}</span>
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
