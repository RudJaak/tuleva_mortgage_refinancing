import { FormattedMessage, useIntl } from 'react-intl'
import { Loader } from '../common/Loader'
import { useCalculatorSettings } from '../calculator/settings'
import { useMembership } from './api'

/** Shown right after login: checks whether the person is a Tuleva member, as joining needs it. */
export function MembershipCheck() {
  const intl = useIntl()
  const membership = useMembership()
  const fee = intl.formatNumber(useCalculatorSettings().membershipFee, {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
  })

  if (membership.isPending) {
    return (
      <div className="d-flex align-items-center gap-3 membership-check">
        <Loader
          className="membership-loader"
          label={intl.formatMessage({ id: 'membership.checking' })}
        />
        <span className="text-body-secondary">
          <FormattedMessage id="membership.checking" />
        </span>
      </div>
    )
  }
  if (membership.data?.member) {
    return (
      <div
        className="d-flex align-items-start gap-3 text-start membership-check"
        data-testid="membership-member"
      >
        <span className="membership-mark" aria-hidden="true">
          ✓
        </span>
        <div>
          <div className="fw-bold text-success">
            <FormattedMessage id="membership.member" />
          </div>
          <div className="small text-body-secondary">
            <FormattedMessage id="membership.memberDetail" values={{ fee }} />
          </div>
        </div>
      </div>
    )
  }
  return (
    <div className="alert alert-info text-start mb-0" data-testid="membership-not-member">
      <FormattedMessage id="membership.notMember" values={{ fee }} />
    </div>
  )
}
