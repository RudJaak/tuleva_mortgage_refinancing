package ee.tuleva.mortgage.document;

/** How far an uploaded document can be trusted, strongest first. */
public enum VerificationLevel {
  /** A valid seal from a configured bank covers every file. */
  BANK_SEALED,
  /** A valid seal, but not from a bank we know. */
  UNTRUSTED_SEAL,
  /** Valid signatures of people (and possibly a bank), e.g. a contract signed by its parties. */
  SIGNED,
  /** The container's seal does not validate, so the content may have been altered. */
  INVALID_SEAL,
  /** A container without any signature. */
  UNSIGNED_CONTAINER,
  /** A bare PDF: nothing proves where it came from. */
  PLAIN_PDF
}
