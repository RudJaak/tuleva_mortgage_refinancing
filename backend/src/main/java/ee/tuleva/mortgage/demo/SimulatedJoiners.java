package ee.tuleva.mortgage.demo;

import ee.tuleva.mortgage.pool.PoolTotals;
import ee.tuleva.mortgage.pool.PoolTotalsAdjustment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

/**
 * Keeps the landing-page tracker alive in a demo: every 15–45 s one more borrower "joins" with a
 * random balance. Held in memory only, so the pool falls back to the database totals on restart.
 */
@RequiredArgsConstructor
class SimulatedJoiners implements PoolTotalsAdjustment {

  private final DemoProperties.SimulatedJoiners settings;
  private final TaskScheduler scheduler;
  private final Clock clock;
  private final RandomGenerator random;
  private final AtomicReference<PoolTotals> totals = new AtomicReference<>(PoolTotals.EMPTY);

  @Override
  public PoolTotals totals() {
    return totals.get();
  }

  @EventListener(ApplicationReadyEvent.class)
  void start() {
    scheduleNext();
  }

  void join() {
    totals.updateAndGet(current -> current.plus(new PoolTotals(1, nextBalance())));
  }

  Duration nextInterval() {
    long min = settings.minInterval().toMillis();
    long max = settings.maxInterval().toMillis();
    return Duration.ofMillis(random.nextLong(min, max + 1));
  }

  BigDecimal nextBalance() {
    double min = settings.minBalance().doubleValue();
    double max = settings.maxBalance().doubleValue();
    return BigDecimal.valueOf(random.nextDouble(min, max)).setScale(-2, RoundingMode.HALF_UP);
  }

  private void scheduleNext() {
    scheduler.schedule(
        () -> {
          join();
          scheduleNext();
        },
        clock.instant().plus(nextInterval()));
  }
}
