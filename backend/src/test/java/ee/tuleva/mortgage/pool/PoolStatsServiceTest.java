package ee.tuleva.mortgage.pool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PoolStatsServiceTest {

  private final PoolProperties properties =
      new PoolProperties(
          OffsetDateTime.parse("2026-11-10T00:00:00+02:00"),
          1000,
          List.of(
              new PoolProperties.Milestone(2500, new BigDecimal("1.30")),
              new PoolProperties.Milestone(1000, new BigDecimal("1.35")),
              new PoolProperties.Milestone(5000, new BigDecimal("1.20"))),
          10);

  private final PoolStatsService service =
      new PoolStatsService(mock(CommitmentRepository.class), properties, List.of());

  @Test
  void beforeTheMinimumNoMilestoneIsReachedAndTheFirstIsNext() {
    var stats = service.statsFor(new PoolTotals(520, new BigDecimal("60000000")));

    assertThat(stats.joinedCount()).isEqualTo(520);
    assertThat(stats.minimumMembers()).isEqualTo(1000);
    assertThat(stats.minimumReached()).isFalse();
    assertThat(stats.currentMilestone()).isNull();
    assertThat(stats.nextMilestone().members()).isEqualTo(1000);
    assertThat(stats.nextMilestone().margin()).isEqualByComparingTo("1.35");
  }

  @Test
  void milestonesAreOrderedByPeople() {
    var stats = service.statsFor(new PoolTotals(520, new BigDecimal("60000000")));

    assertThat(stats.milestones())
        .extracting(PoolStats.MilestoneStatus::members)
        .containsExactly(1000, 2500, 5000);
  }

  @Test
  void theHighestReachedMilestoneSetsTheCurrentMargin() {
    var stats = service.statsFor(new PoolTotals(3000, new BigDecimal("320000000")));

    assertThat(stats.minimumReached()).isTrue();
    assertThat(stats.currentMilestone().margin()).isEqualByComparingTo("1.30");
    assertThat(stats.nextMilestone().margin()).isEqualByComparingTo("1.20");
  }

  @Test
  void peopleCountNotMoney() {
    // a thousand small loans reach the minimum; a few large ones do not
    assertThat(service.statsFor(new PoolTotals(1000, new BigDecimal("20000000"))).minimumReached())
        .isTrue();
    assertThat(service.statsFor(new PoolTotals(999, new BigDecimal("900000000"))).minimumReached())
        .isFalse();
  }

  @Test
  void afterTheLastMilestoneThereIsNoNext() {
    var stats = service.statsFor(new PoolTotals(5000, new BigDecimal("510000000")));

    assertThat(stats.currentMilestone().margin()).isEqualByComparingTo("1.20");
    assertThat(stats.nextMilestone()).isNull();
  }

  @Test
  void hidesCountAndVolumeWhileThePoolIsTooSmallToStayAnonymous() {
    var stats = service.statsFor(new PoolTotals(9, new BigDecimal("1200000")));

    assertThat(stats.joinedCount()).isNull();
    assertThat(stats.committedVolume()).isNull();
    assertThat(stats.nextMilestone().members()).isEqualTo(1000);
  }

  @Test
  void adjustmentsAreAddedOnTopOfTheSignedCommitments() {
    var commitments = mock(CommitmentRepository.class);
    given(commitments.signedTotals()).willReturn(new PoolTotals(520, new BigDecimal("60000000")));
    PoolTotalsAdjustment twoMore = () -> new PoolTotals(2, new BigDecimal("400000"));
    var adjusted = new PoolStatsService(commitments, properties, List.of(twoMore));

    var stats = adjusted.currentStats();

    assertThat(stats.joinedCount()).isEqualTo(522);
    assertThat(stats.committedVolume()).isEqualByComparingTo("60400000");
  }
}
