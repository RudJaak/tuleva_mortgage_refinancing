package ee.tuleva.mortgage.demo;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
record DemoProperties(
    boolean seedEnabled,
    boolean freshStartOnLogin,
    int seedBorrowers,
    BigDecimal seedVolume,
    SimulatedJoiners simulatedJoiners) {

  record SimulatedJoiners(
      boolean enabled,
      Duration minInterval,
      Duration maxInterval,
      BigDecimal minBalance,
      BigDecimal maxBalance) {}
}
