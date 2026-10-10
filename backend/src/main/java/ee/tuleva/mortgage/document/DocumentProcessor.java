package ee.tuleva.mortgage.document;

/**
 * Turns a received document into domain data. Implemented by the module that owns the document type
 * (e.g. {@code statement} for account statements), so this module stays independent of them.
 */
public interface DocumentProcessor {

  DocumentType type();

  /**
   * Whether {@code app.documents.require-bank-seal} applies to this type. A statement can come
   * sealed by the bank; a contract is signed by people, so it never carries a bank seal.
   */
  default boolean bankSealCanBeRequired() {
    return false;
  }

  /** Returns the response body shown to the borrower for confirmation. */
  Object process(ReceivedDocument document);

  /**
   * The stored result of a document processed earlier, e.g. before a restart cleared the in-memory
   * one. Anything not stored (such as statement transactions) is left out.
   */
  java.util.Optional<Object> storedResult(java.util.UUID documentId);
}
