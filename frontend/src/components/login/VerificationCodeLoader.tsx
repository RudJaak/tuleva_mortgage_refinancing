// Adapted from TulevaEE/onboarding-client src/components/common/authenticationLoader (MIT, (c) 2017 Tuleva).
import { FormattedMessage } from 'react-intl'
import { CancelButton } from '../common/CancelButton'
import { FocusedParagraph } from '../common/FocusedParagraph'
import { Loader } from '../common/Loader'

export function VerificationCodeLoader({
  verificationCode,
  verifying = false,
  onCancel,
}: {
  verificationCode: string | null
  verifying?: boolean
  onCancel: () => void
}) {
  return (
    <>
      {verifying && (
        <FocusedParagraph className="m-0 mb-4 text-balance">
          <FormattedMessage id="login.verifying" />
        </FocusedParagraph>
      )}
      {verificationCode && (
        <>
          <FocusedParagraph className="m-0 mb-4 text-balance">
            <FormattedMessage id="login.control.code" />
          </FocusedParagraph>
          <div className="display-2 fw-bold mb-4" data-testid="verification-code">
            {verificationCode}
          </div>
        </>
      )}
      <Loader className="align-middle" />
      {verificationCode && <CancelButton onCancel={onCancel} className="mt-4" />}
    </>
  )
}
