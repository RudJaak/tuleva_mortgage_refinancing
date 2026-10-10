package ee.tuleva.mortgage.extraction;

/** A document could not be read; the message is safe to show to the borrower. */
public class ExtractionException extends RuntimeException {

  public ExtractionException(String message) {
    super(message);
  }

  public ExtractionException(String message, Throwable cause) {
    super(message, cause);
  }
}
