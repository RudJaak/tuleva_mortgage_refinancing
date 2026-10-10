package ee.tuleva.mortgage.loan;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** The borrower's confirmed loan figures, for modules that act on them (the commitment). */
@Component
@RequiredArgsConstructor
public class ConfirmedLoans {

  /** {@code balance} is stated or estimated; null when neither is possible. */
  public record Figures(
      @Nullable String lender,
      @Nullable BigDecimal balance,
      @Nullable BigDecimal margin,
      @Nullable Integer remainingMonths) {}

  private final LoanService loans;

  public Optional<Figures> of(UUID borrowerId) {
    return loans
        .current(borrowerId)
        .map(
            view ->
                new Figures(
                    view.terms().lender(),
                    view.summary().balance(),
                    view.terms().margin(),
                    view.summary().remainingMonths()));
  }
}
