package ee.tuleva.mortgage.pool;

import ee.tuleva.mortgage.pool.PoolStats.MilestoneStatus;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PoolStatsService {

  private final CommitmentRepository commitments;
  private final PoolProperties properties;
  private final List<PoolTotalsAdjustment> adjustments;

  public PoolStats currentStats() {
    return statsFor(
        adjustments.stream()
            .map(PoolTotalsAdjustment::totals)
            .reduce(commitments.signedTotals(), PoolTotals::plus));
  }

  PoolStats statsFor(PoolTotals totals) {
    List<MilestoneStatus> milestones =
        properties.milestones().stream()
            .sorted(Comparator.comparingInt(PoolProperties.Milestone::members))
            .map(
                milestone ->
                    new MilestoneStatus(
                        milestone.members(),
                        milestone.margin(),
                        totals.count() >= milestone.members()))
            .toList();
    MilestoneStatus current =
        milestones.stream().filter(MilestoneStatus::reached).reduce((_, b) -> b).orElse(null);
    MilestoneStatus next = milestones.stream().filter(m -> !m.reached()).findFirst().orElse(null);
    boolean visible = totals.count() >= properties.privacyThreshold();
    return new PoolStats(
        visible ? totals.count() : null,
        visible ? totals.volume() : null,
        properties.minimumMembers(),
        totals.count() >= properties.minimumMembers(),
        properties.deadline(),
        milestones,
        current,
        next);
  }
}
