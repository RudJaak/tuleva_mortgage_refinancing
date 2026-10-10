package ee.tuleva.mortgage.pool;

import java.math.BigDecimal;

public record PoolTotals(long count, BigDecimal volume) {

  public static final PoolTotals EMPTY = new PoolTotals(0, BigDecimal.ZERO);

  public PoolTotals plus(PoolTotals other) {
    return new PoolTotals(count + other.count, volume.add(other.volume));
  }
}
