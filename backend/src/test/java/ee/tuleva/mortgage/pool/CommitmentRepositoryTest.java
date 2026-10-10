package ee.tuleva.mortgage.pool;

import static org.assertj.core.api.Assertions.assertThat;

import ee.tuleva.mortgage.TestcontainersConfiguration;
import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CommitmentRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-10-10T10:00:00Z");

  @Autowired private BorrowerRepository borrowers;
  @Autowired private CommitmentRepository commitments;

  @Test
  void totalsAreZeroForAnEmptyPool() {
    var totals = commitments.signedTotals();

    assertThat(totals.count()).isZero();
    assertThat(totals.volume()).isEqualByComparingTo("0");
  }

  @Test
  void totalsCountOnlySignedCommitments() {
    commitments.save(signedCommitment("150000.00"));
    commitments.save(signedCommitment("90000.50"));
    var withdrawn = signedCommitment("70000.00");
    withdrawn.withdraw(NOW);
    commitments.save(withdrawn);

    var totals = commitments.signedTotals();

    assertThat(totals.count()).isEqualTo(2);
    assertThat(totals.volume()).isEqualByComparingTo("240000.50");
  }

  private Commitment signedCommitment(String balance) {
    var borrower = borrowers.save(Borrower.synthetic(NOW));
    return Commitment.signed(
        borrower.getId(), new BigDecimal(balance), new BigDecimal("1.85"), NOW);
  }
}
