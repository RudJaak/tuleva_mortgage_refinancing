package ee.tuleva.mortgage.creditcheck;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CreditCheckRepository extends JpaRepository<CreditCheck, UUID> {

  Optional<CreditCheck> findByBorrowerId(UUID borrowerId);

  void deleteByBorrowerId(UUID borrowerId);
}
