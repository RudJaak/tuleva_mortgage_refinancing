package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.borrower.BorrowerDataReset;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LoanDataEraser {

  private final LoanRepository loans;
  private final StoredLoanContractRepository contracts;

  @EventListener
  @Order(BorrowerDataReset.LOAN_ORDER)
  void on(BorrowerDataReset reset) {
    loans.deleteByBorrowerId(reset.borrowerId());
    contracts.deleteByBorrowerId(reset.borrowerId());
  }
}
