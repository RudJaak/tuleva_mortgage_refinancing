package ee.tuleva.mortgage.pool;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pool")
public record PoolProperties(
    OffsetDateTime deadline,
    BigDecimal minimumVolume,
    List<Milestone> milestones,
    int privacyThreshold) {

  public record Milestone(BigDecimal volume, BigDecimal margin) {}
}
