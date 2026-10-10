package ee.tuleva.mortgage.loan;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LoanRepository extends JpaRepository<Loan, UUID> {

  Optional<Loan> findByBorrowerId(UUID borrowerId);
}
