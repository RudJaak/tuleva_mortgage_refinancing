package ee.tuleva.mortgage.borrower;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerRepository extends JpaRepository<Borrower, UUID> {

  long countBySyntheticTrue();
}
