import { FormattedList, FormattedMessage } from 'react-intl'
import type { Verification } from './api'

export function SealBadge({ verification }: { verification: Verification }) {
  const verified =
    verification.level === 'BANK_SEALED' ||
    (verification.level === 'SIGNED' && verification.signedByBorrower !== false)
  return (
    <span
      className={`seal-badge ${verified ? 'seal-badge-verified' : 'seal-badge-unverified'}`}
      data-testid="seal-badge"
    >
      {verified && <span aria-hidden="true">✓ </span>}
      {verification.level === 'SIGNED' && verification.signers.length > 0 ? (
        <FormattedMessage
          id={
            verification.signedByBorrower === false
              ? 'documents.seal.SIGNED_BY_OTHERS'
              : 'documents.seal.SIGNED'
          }
          values={{
            signers: (
              <FormattedList type="conjunction" value={verification.signers.map((s) => s.name)} />
            ),
          }}
        />
      ) : (
        <FormattedMessage
          id={`documents.seal.${verification.level}`}
          values={{ bank: verification.bankName }}
        />
      )}
    </span>
  )
}
