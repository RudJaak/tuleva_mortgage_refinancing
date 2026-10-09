import { FormattedMessage, useIntl } from 'react-intl'
import { flowSteps } from '../paths'

export function Stepper({ currentIndex }: { currentIndex: number }) {
  const intl = useIntl()
  return (
    <nav
      aria-label={intl.formatMessage(
        { id: 'flow.stepOf' },
        { current: currentIndex + 1, total: flowSteps.length },
      )}
    >
      <p className="d-sm-none text-body-secondary mb-0">
        <FormattedMessage
          id="flow.stepOf"
          values={{ current: currentIndex + 1, total: flowSteps.length }}
        />
        {' · '}
        <FormattedMessage id={flowSteps[currentIndex].labelId} />
      </p>
      <ol className="stepper d-none d-sm-flex list-unstyled mb-0">
        {flowSteps.map((step, index) => (
          <li
            key={step.path}
            className={
              index < currentIndex
                ? 'stepper-done'
                : index === currentIndex
                  ? 'stepper-current'
                  : ''
            }
            aria-current={index === currentIndex ? 'step' : undefined}
          >
            <span className="stepper-index">{index + 1}</span>
            <FormattedMessage id={step.labelId} />
          </li>
        ))}
      </ol>
    </nav>
  )
}
