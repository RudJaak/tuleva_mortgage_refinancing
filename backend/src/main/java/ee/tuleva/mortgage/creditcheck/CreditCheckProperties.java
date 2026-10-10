package ee.tuleva.mortgage.creditcheck;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.credit-check")
record CreditCheckProperties(Duration stubDelay) {}
