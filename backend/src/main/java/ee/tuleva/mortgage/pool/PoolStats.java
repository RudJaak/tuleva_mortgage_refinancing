package ee.tuleva.mortgage.pool;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record PoolStats(
    @Nullable Long joinedCount,
    @Nullable BigDecimal committedVolume,
    int minimumMembers,
    boolean minimumReached,
    OffsetDateTime deadline,
    List<MilestoneStatus> milestones,
    @Nullable MilestoneStatus currentMilestone,
    @Nullable MilestoneStatus nextMilestone) {

  public record MilestoneStatus(int members, BigDecimal margin, boolean reached) {}
}
