package ee.tuleva.mortgage.document;

/**
 * Turns a received document into domain data. Implemented by the module that owns the document type
 * (e.g. {@code statement} for account statements), so this module stays independent of them.
 */
public interface DocumentProcessor {

  DocumentType type();

  /** Returns the response body shown to the borrower for confirmation. */
  Object process(ReceivedDocument document);
}
