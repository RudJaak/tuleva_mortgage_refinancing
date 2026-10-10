package ee.tuleva.mortgage.statement;

import lombok.Getter;

@Getter
class SealedStatementException extends RuntimeException {

  enum Reason {
    NOT_A_CONTAINER,
    UNSIGNED,
    INVALID_SIGNATURE,
    NOT_A_BANK_SEAL
  }

  private final Reason reason;

  SealedStatementException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  SealedStatementException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }
}
