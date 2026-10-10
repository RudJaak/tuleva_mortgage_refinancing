package ee.tuleva.mortgage.demo;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
record DemoProperties(boolean seedEnabled, int seedBorrowers, BigDecimal seedVolume) {}
