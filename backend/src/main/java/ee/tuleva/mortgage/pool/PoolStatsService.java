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

  public PoolStats currentStats() {
    return statsFor(commitments.signedTotals());
  }

  PoolStats statsFor(PoolTotals totals) {
    List<MilestoneStatus> milestones =
        properties.milestones().stream()
            .sorted(Comparator.comparing(PoolProperties.Milestone::volume))
            .map(
                milestone ->
                    new MilestoneStatus(
                        milestone.volume(),
                        milestone.margin(),
                        totals.volume().compareTo(milestone.volume()) >= 0))
            .toList();
    MilestoneStatus current =
        milestones.stream().filter(MilestoneStatus::reached).reduce((a, b) -> b).orElse(null);
    MilestoneStatus next = milestones.stream().filter(m -> !m.reached()).findFirst().orElse(null);
    boolean visible = totals.count() >= properties.privacyThreshold();
    return new PoolStats(
        visible ? totals.count() : null,
        visible ? totals.volume() : null,
        properties.minimumVolume(),
        totals.volume().compareTo(properties.minimumVolume()) >= 0,
        properties.deadline(),
        milestones,
        current,
        next);
  }
}
