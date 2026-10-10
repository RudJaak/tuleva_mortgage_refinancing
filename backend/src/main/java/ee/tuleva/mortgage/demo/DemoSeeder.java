package ee.tuleva.mortgage.demo;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerRepository;
import ee.tuleva.mortgage.pool.Commitment;
import ee.tuleva.mortgage.pool.CommitmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class DemoSeeder {

  private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
  private static final double TYPICAL_BALANCE = 110_000;
  private static final double BALANCE_SPREAD = 0.45;
  private static final double MIN_BALANCE = 20_000;

  private final BorrowerRepository borrowers;
  private final CommitmentRepository commitments;
  private final Clock clock;

  DemoSeeder(BorrowerRepository borrowers, CommitmentRepository commitments, Clock clock) {
    this.borrowers = borrowers;
    this.commitments = commitments;
    this.clock = clock;
  }

  @Transactional
  boolean seedIfEmpty(int borrowerCount, BigDecimal totalVolume) {
    if (borrowers.countBySyntheticTrue() > 0) {
      return false;
    }
    Random random = new Random(42);
    List<BigDecimal> balances = balances(random, borrowerCount, totalVolume);
    Instant now = clock.instant();
    List<Borrower> seededBorrowers = new ArrayList<>();
    List<Commitment> seededCommitments = new ArrayList<>();
    for (BigDecimal balance : balances) {
      Instant signedAt = now.minus(Duration.ofMinutes(random.nextInt(30 * 24 * 60)));
      Borrower borrower = Borrower.synthetic(signedAt);
      seededBorrowers.add(borrower);
      seededCommitments.add(Commitment.signed(borrower.getId(), balance, margin(random), signedAt));
    }
    borrowers.saveAll(seededBorrowers);
    commitments.saveAll(seededCommitments);
    log.info("Seeded {} synthetic demo commitments", borrowerCount);
    return true;
  }

  static List<BigDecimal> balances(Random random, int count, BigDecimal totalVolume) {
    double[] raw = new double[count];
    double sum = 0;
    for (int i = 0; i < count; i++) {
      raw[i] =
          Math.max(MIN_BALANCE, TYPICAL_BALANCE * Math.exp(BALANCE_SPREAD * random.nextGaussian()));
      sum += raw[i];
    }
    double scale = totalVolume.doubleValue() / sum;
    List<BigDecimal> balances = new ArrayList<>();
    BigDecimal assigned = BigDecimal.ZERO;
    for (int i = 0; i < count - 1; i++) {
      BigDecimal balance = BigDecimal.valueOf(raw[i] * scale).setScale(0, RoundingMode.HALF_UP);
      balances.add(balance);
      assigned = assigned.add(balance);
    }
    balances.add(totalVolume.subtract(assigned).setScale(0, RoundingMode.HALF_UP));
    return balances;
  }

  private static BigDecimal margin(Random random) {
    int steps = random.nextInt(18);
    return BigDecimal.valueOf(145 + steps * 5L, 2);
  }
}
