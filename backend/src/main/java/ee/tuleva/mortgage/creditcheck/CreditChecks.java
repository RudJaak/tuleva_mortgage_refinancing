package ee.tuleva.mortgage.creditcheck;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerDataReset;
import ee.tuleva.mortgage.borrower.BorrowerService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The payment-default check (D32): run once the documents are in, because a query costs money, and
 * only with the borrower's consent. Only the outcome is kept, never the report.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(CreditCheckProperties.class)
public class CreditChecks {

  public record View(String result, int activeDefaults, String source, Instant checkedAt) {}

  private final CreditCheckProvider provider;
  private final CreditCheckRepository checks;
  private final BorrowerService borrowers;
  private final Clock clock;

  /** Whether the borrower has been checked; signing the commitment needs it. */
  @Transactional(readOnly = true)
  public boolean done(UUID borrowerId) {
    return checks.findByBorrowerId(borrowerId).isPresent();
  }

  @Transactional(readOnly = true)
  Optional<View> current(UUID borrowerId) {
    return checks.findByBorrowerId(borrowerId).map(CreditChecks::view);
  }

  @Transactional
  View run(UUID borrowerId) {
    String personalCode =
        borrowers
            .find(borrowerId)
            .map(Borrower::getPersonalCode)
            .orElseThrow(() -> new IllegalStateException("Logged-in borrower without a code"));
    var report = provider.check(personalCode);
    checks.findByBorrowerId(borrowerId).ifPresent(checks::delete);
    checks.flush();
    return view(checks.save(CreditCheck.of(borrowerId, report, clock.instant())));
  }

  @EventListener
  @Order(BorrowerDataReset.CREDIT_CHECK_ORDER)
  void on(BorrowerDataReset reset) {
    checks.deleteByBorrowerId(reset.borrowerId());
  }

  private static View view(CreditCheck check) {
    return new View(
        check.getResult().name(),
        check.getActiveDefaults(),
        check.getSource(),
        check.getCheckedAt());
  }
}
