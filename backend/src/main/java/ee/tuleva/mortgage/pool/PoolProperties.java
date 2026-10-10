package ee.tuleva.mortgage.pool;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The pool is counted in people, not euros (D31), so every borrower feels welcome whatever their
 * balance. Below {@code minimumMembers} no tender happens; each milestone's margin is the target
 * once that many people have joined.
 */
@ConfigurationProperties(prefix = "app.pool")
public record PoolProperties(
    OffsetDateTime deadline, int minimumMembers, List<Milestone> milestones, int privacyThreshold) {

  public record Milestone(int members, BigDecimal margin) {}
}
