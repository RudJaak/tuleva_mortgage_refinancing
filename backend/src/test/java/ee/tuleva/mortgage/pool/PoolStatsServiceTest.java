package ee.tuleva.mortgage.pool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PoolStatsServiceTest {

  private final PoolProperties properties =
      new PoolProperties(
          OffsetDateTime.parse("2026-12-31T23:59:59+02:00"),
          new BigDecimal("100000000"),
          List.of(
              new PoolProperties.Milestone(new BigDecimal("300000000"), new BigDecimal("1.00")),
              new PoolProperties.Milestone(new BigDecimal("100000000"), new BigDecimal("1.20")),
              new PoolProperties.Milestone(new BigDecimal("500000000"), new BigDecimal("0.80"))),
          10);

  private final PoolStatsService service =
      new PoolStatsService(mock(CommitmentRepository.class), properties);

  @Test
  void beforeTheMinimumNoMilestoneIsReachedAndTheFirstIsNext() {
    var stats = service.statsFor(new PoolTotals(520, new BigDecimal("60000000")));

    assertThat(stats.joinedCount()).isEqualTo(520);
    assertThat(stats.committedVolume()).isEqualByComparingTo("60000000");
    assertThat(stats.minimumReached()).isFalse();
    assertThat(stats.currentMilestone()).isNull();
    assertThat(stats.nextMilestone().volume()).isEqualByComparingTo("100000000");
    assertThat(stats.nextMilestone().margin()).isEqualByComparingTo("1.20");
  }

  @Test
  void milestonesAreOrderedByVolume() {
    var stats = service.statsFor(new PoolTotals(520, new BigDecimal("60000000")));

    assertThat(stats.milestones())
        .extracting(PoolStats.MilestoneStatus::volume)
        .map(BigDecimal::intValue)
        .containsExactly(100_000_000, 300_000_000, 500_000_000);
  }

  @Test
  void theHighestReachedMilestoneSetsTheCurrentMargin() {
    var stats = service.statsFor(new PoolTotals(3000, new BigDecimal("320000000")));

    assertThat(stats.minimumReached()).isTrue();
    assertThat(stats.currentMilestone().margin()).isEqualByComparingTo("1.00");
    assertThat(stats.nextMilestone().margin()).isEqualByComparingTo("0.80");
  }

  @Test
  void reachingExactlyTheVolumeCountsAsReached() {
    var stats = service.statsFor(new PoolTotals(900, new BigDecimal("100000000")));

    assertThat(stats.minimumReached()).isTrue();
    assertThat(stats.currentMilestone().volume()).isEqualByComparingTo("100000000");
  }

  @Test
  void afterTheLastMilestoneThereIsNoNext() {
    var stats = service.statsFor(new PoolTotals(5000, new BigDecimal("510000000")));

    assertThat(stats.currentMilestone().margin()).isEqualByComparingTo("0.80");
    assertThat(stats.nextMilestone()).isNull();
  }

  @Test
  void hidesCountAndVolumeWhileThePoolIsTooSmallToStayAnonymous() {
    var stats = service.statsFor(new PoolTotals(9, new BigDecimal("1200000")));

    assertThat(stats.joinedCount()).isNull();
    assertThat(stats.committedVolume()).isNull();
    assertThat(stats.nextMilestone().volume()).isEqualByComparingTo("100000000");
  }
}
