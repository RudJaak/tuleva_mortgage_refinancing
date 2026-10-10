import { FormattedList, FormattedMessage } from 'react-intl'
import type { ReactNode } from 'react'
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
      <FormattedMessage id={messageId(verification)} values={values(verification)} />
    </span>
  )
}

/**
 * A signed contract names its signers while the backend still has them in memory; after a restart
 * only whether the borrower signed is known, so the badge falls back to a neutral wording.
 */
function messageId(verification: Verification) {
  if (verification.level !== 'SIGNED') {
    return `documents.seal.${verification.level}`
  }
  const named = verification.signers.length > 0
  const byOthers = verification.signedByBorrower === false
  return `documents.seal.SIGNED${byOthers ? '_BY_OTHERS' : ''}${named ? '' : '_UNNAMED'}`
}

function values(verification: Verification): Record<string, ReactNode> {
  return {
    bank: verification.bankName,
    signers: <FormattedList type="conjunction" value={verification.signers.map((s) => s.name)} />,
  }
}
