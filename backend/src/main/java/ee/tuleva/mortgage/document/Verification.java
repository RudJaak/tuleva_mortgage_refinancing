package ee.tuleva.mortgage.document;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * How an upload was verified. {@code signers} names who signed or sealed it; {@code
 * signedByBorrower} says whether the logged-in borrower is one of them (null when nobody signed).
 * Personal codes are compared on the server and never leave it.
 */
public record Verification(
    VerificationLevel level,
    @Nullable String bankName,
    @Nullable String sealedBy,
    @Nullable Instant sealedAt,
    List<Signer> signers,
    @Nullable Boolean signedByBorrower) {

  public record Signer(String name, Kind kind) {
    public enum Kind {
      PERSON,
      ORGANISATION
    }
  }

  static Verification of(VerificationLevel level) {
    return new Verification(level, null, null, null, List.of(), null);
  }

  Verification withSignedByBorrower(@Nullable Boolean signedByBorrower) {
    return new Verification(level, bankName, sealedBy, sealedAt, signers, signedByBorrower);
  }
}
