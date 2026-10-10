package ee.tuleva.mortgage.pool;

import ee.tuleva.mortgage.borrower.BorrowerDataReset;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CommitmentDataEraser {

  private final CommitmentRepository commitments;

  @EventListener
  @Order(BorrowerDataReset.COMMITMENT_ORDER)
  void on(BorrowerDataReset reset) {
    commitments.deleteByBorrowerId(reset.borrowerId());
  }
}
