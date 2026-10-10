import { FormattedMessage } from 'react-intl'
import { Link } from 'react-router-dom'
import { FocusedParagraph } from '../common/FocusedParagraph'
import { MembershipCheck } from '../membership/MembershipCheck'
import { paths } from '../paths'

export function LoginSuccess({ firstName }: { firstName: string | null }) {
  return (
    <div className="d-flex flex-column align-items-center gap-3">
      <span className="login-success-mark" aria-hidden="true">
        ✓
      </span>
      <h2 className="m-0">
        {firstName ? (
          <FormattedMessage id="login.success.greeting" values={{ firstName }} />
        ) : (
          <FormattedMessage id="login.success.title" />
        )}
      </h2>
      <MembershipCheck />
      <FocusedParagraph className="m-0 text-body-secondary">
        <FormattedMessage id="login.success.lead" />
      </FocusedParagraph>
      <Link to={paths.loan} className="btn btn-primary btn-lg w-100 mt-2">
        <FormattedMessage id="login.success.continue" />
      </Link>
    </div>
  )
}
