// Adapted from TulevaEE/onboarding-client src/components/login/smartId/SmartIdDeviceLinkLogin.tsx (MIT, (c) 2017 Tuleva).
import { QRCodeSVG } from 'qrcode.react'
import { FormattedMessage, useIntl } from 'react-intl'
import { CancelButton } from '../common/CancelButton'
import { FocusedParagraph } from '../common/FocusedParagraph'
import { Loader } from '../common/Loader'
import { ScanFrame } from './ScanFrame'

const MODULES_IN_A_SMART_ID_DEVICE_LINK_QR_CODE = 53
const SMART_ID_MINIMUM_PIXELS_PER_MODULE = 6
const QR_CODE_PIXELS =
  MODULES_IN_A_SMART_ID_DEVICE_LINK_QR_CODE * SMART_ID_MINIMUM_PIXELS_PER_MODULE

export function SmartIdQrLogin({
  deviceLink,
  onCancel,
}: {
  deviceLink: string | null
  onCancel: () => void
}) {
  const { formatMessage } = useIntl()
  return (
    <>
      <FocusedParagraph className="m-0 mb-4 text-pretty">
        <FormattedMessage id="login.smart.id.qr.instructions" />
      </FocusedParagraph>
      <ScanFrame>
        <div
          className="d-flex align-items-center justify-content-center mx-auto"
          style={{ width: QR_CODE_PIXELS, maxWidth: '100%', aspectRatio: '1' }}
        >
          {deviceLink ? (
            <QRCodeSVG
              value={deviceLink}
              size={QR_CODE_PIXELS}
              level="L"
              bgColor="#ffffff"
              style={{ maxWidth: '100%', height: 'auto', aspectRatio: '1' }}
              role="img"
              aria-label={formatMessage({ id: 'login.smart.id.qr.instructions' })}
            />
          ) : (
            <Loader />
          )}
        </div>
      </ScanFrame>
      <CancelButton onCancel={onCancel} className="mt-4" />
    </>
  )
}
