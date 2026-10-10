package ee.tuleva.mortgage.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DemoSeederTest {

  @Test
  void balancesAddUpExactlyToTheTargetVolume() {
    var balances = DemoSeeder.balances(new Random(42), 520, new BigDecimal("60000000"));

    assertThat(balances).hasSize(520);
    assertThat(balances.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("60000000");
  }

  @Test
  void balancesLookLikeRealisticMortgages() {
    var balances = DemoSeeder.balances(new Random(42), 520, new BigDecimal("60000000"));

    assertThat(balances)
        .allSatisfy(
            b -> assertThat(b).isBetween(new BigDecimal("15000"), new BigDecimal("600000")));
  }
}
