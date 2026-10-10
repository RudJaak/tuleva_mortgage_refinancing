package ee.tuleva.mortgage.document;

import lombok.Getter;

@Getter
public class DocumentRejectedException extends RuntimeException {

  public enum Reason {
    UNSUPPORTED_FORMAT,
    UNSUPPORTED_TYPE,
    NOT_A_CONTAINER,
    NO_USABLE_FILE,
    SEAL_REQUIRED,
    EXTRACTION_FAILED,
    PROCESSING_FAILED,
    INTERRUPTED
  }

  private final Reason reason;

  public DocumentRejectedException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public DocumentRejectedException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }
}
