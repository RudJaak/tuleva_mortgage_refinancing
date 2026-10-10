package ee.tuleva.mortgage.pool;

/** Totals added on top of the signed commitments, e.g. simulated joiners in a live demo. */
public interface PoolTotalsAdjustment {

  PoolTotals totals();
}
