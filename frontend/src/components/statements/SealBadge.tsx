import { FormattedMessage } from 'react-intl'
import type { Verification } from './api'

export function SealBadge({ verification }: { verification: Verification }) {
  const sealed = verification.level === 'BANK_SEALED'
  return (
    <span
      className={`seal-badge ${sealed ? 'seal-badge-verified' : 'seal-badge-unverified'}`}
      data-testid="seal-badge"
    >
      {sealed && <span aria-hidden="true">✓ </span>}
      <FormattedMessage
        id={`statements.seal.${verification.level}`}
        values={{ bank: verification.bankName }}
      />
    </span>
  )
}
