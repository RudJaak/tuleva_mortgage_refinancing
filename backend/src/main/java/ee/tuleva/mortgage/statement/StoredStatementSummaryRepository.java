package ee.tuleva.mortgage.statement;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StoredStatementSummaryRepository extends JpaRepository<StoredStatementSummary, UUID> {}
