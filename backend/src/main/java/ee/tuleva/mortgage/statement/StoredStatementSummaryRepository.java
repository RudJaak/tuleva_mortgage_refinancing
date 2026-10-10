package ee.tuleva.mortgage.statement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StoredStatementSummaryRepository extends JpaRepository<StoredStatementSummary, UUID> {

  Optional<StoredStatementSummary> findByDocumentId(UUID documentId);

  List<StoredStatementSummary> findByBorrowerIdOrderByCreatedAtDesc(UUID borrowerId);

  void deleteByBorrowerId(UUID borrowerId);
}
