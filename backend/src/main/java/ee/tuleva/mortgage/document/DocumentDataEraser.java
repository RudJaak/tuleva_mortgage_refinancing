package ee.tuleva.mortgage.document;

import ee.tuleva.mortgage.borrower.BorrowerDataReset;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DocumentDataEraser {

  private final DocumentRepository documents;

  @EventListener
  @Order(BorrowerDataReset.DOCUMENT_ORDER)
  void on(BorrowerDataReset reset) {
    documents.deleteByBorrowerId(reset.borrowerId());
  }
}
