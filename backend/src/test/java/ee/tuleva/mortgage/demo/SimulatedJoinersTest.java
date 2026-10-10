package ee.tuleva.mortgage.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class SimulatedJoinersTest {

  private final SimulatedJoiners joiners =
      new SimulatedJoiners(
          new DemoProperties.SimulatedJoiners(
              true,
              Duration.ofSeconds(15),
              Duration.ofSeconds(45),
              new BigDecimal("100000"),
              new BigDecimal("350000")),
          mock(TaskScheduler.class),
          Clock.systemUTC(),
          new Random(42));

  @Test
  void startsEmpty() {
    assertThat(joiners.totals().count()).isZero();
    assertThat(joiners.totals().volume()).isEqualByComparingTo("0");
  }

  @Test
  void eachJoinAddsOnePersonWithABalanceInRange() {
    for (int i = 0; i < 100; i++) {
      var before = joiners.totals();
      joiners.join();
      var added = joiners.totals().volume().subtract(before.volume());

      assertThat(joiners.totals().count()).isEqualTo(before.count() + 1);
      assertThat(added).isBetween(new BigDecimal("100000"), new BigDecimal("350000"));
    }
  }

  @Test
  void intervalsStayWithinTheConfiguredRange() {
    for (int i = 0; i < 100; i++) {
      assertThat(joiners.nextInterval()).isBetween(Duration.ofSeconds(15), Duration.ofSeconds(45));
    }
  }
}
