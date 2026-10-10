package ee.tuleva.mortgage.pool;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommitmentRepository extends JpaRepository<Commitment, UUID> {

  @Query(
      """
      select new ee.tuleva.mortgage.pool.PoolTotals(count(c), coalesce(sum(c.committedBalance), 0))
      from Commitment c where c.status = ee.tuleva.mortgage.pool.CommitmentStatus.SIGNED
      """)
  PoolTotals signedTotals();
}
