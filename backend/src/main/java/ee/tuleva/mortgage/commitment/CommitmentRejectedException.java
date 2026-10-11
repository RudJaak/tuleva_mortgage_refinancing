package ee.tuleva.mortgage.commitment;

import lombok.Getter;

@Getter
class CommitmentRejectedException extends RuntimeException {

  enum Reason {
    LOAN_NOT_CONFIRMED,
    NO_POSSIBLE_GAIN,
    ALREADY_COMMITTED,
    PHONE_NUMBER_INVALID
  }

  private final Reason reason;

  CommitmentRejectedException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }
}
