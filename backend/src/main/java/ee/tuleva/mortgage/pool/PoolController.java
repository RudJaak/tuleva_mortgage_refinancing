package ee.tuleva.mortgage.pool;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
class PoolController {

  private final PoolStatsService poolStatsService;
  private final PoolProperties poolProperties;
  private final CalculatorProperties calculatorProperties;

  @GetMapping("/pool/stats")
  PoolStats stats() {
    return poolStatsService.currentStats();
  }

  @GetMapping("/config/public")
  PublicConfig publicConfig() {
    return new PublicConfig(
        calculatorProperties,
        new PublicConfig.Pool(
            poolProperties.deadline(),
            poolProperties.minimumVolume(),
            poolProperties.milestones()));
  }

  record PublicConfig(CalculatorProperties calculator, Pool pool) {
    record Pool(
        OffsetDateTime deadline,
        BigDecimal minimumVolume,
        List<PoolProperties.Milestone> milestones) {}
  }
}
