package ee.tuleva.mortgage.statement;

import ee.tuleva.mortgage.borrower.BorrowerDataReset;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StatementDataEraser {

  private final StoredStatementSummaryRepository summaries;

  @EventListener
  @Order(BorrowerDataReset.STATEMENT_ORDER)
  void on(BorrowerDataReset reset) {
    summaries.deleteByBorrowerId(reset.borrowerId());
  }
}
