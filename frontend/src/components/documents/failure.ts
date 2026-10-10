const knownReasons = [
  'UNSUPPORTED_FORMAT',
  'NOT_A_CONTAINER',
  'NO_USABLE_FILE',
  'SEAL_REQUIRED',
  'EXTRACTION_FAILED',
  'INTERRUPTED',
] as const

/** Translation id for a backend rejection reason; anything unexpected gets the generic message. */
export function failureMessageId(reason: string | undefined) {
  return knownReasons.includes(reason as (typeof knownReasons)[number])
    ? `documents.failure.${reason}`
    : 'documents.failure.PROCESSING_FAILED'
}
