package ee.tuleva.mortgage.loan;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StoredLoanContractRepository extends JpaRepository<StoredLoanContract, UUID> {}
