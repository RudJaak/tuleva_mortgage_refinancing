package ee.tuleva.mortgage.loan;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StoredLoanContractRepository extends JpaRepository<StoredLoanContract, UUID> {

  Optional<StoredLoanContract> findByDocumentIdAndBorrowerId(UUID documentId, UUID borrowerId);

  Optional<StoredLoanContract> findByDocumentId(UUID documentId);

  Optional<StoredLoanContract> findFirstByBorrowerIdOrderByCreatedAtDesc(UUID borrowerId);

  void deleteByBorrowerId(UUID borrowerId);
}
