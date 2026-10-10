package ee.tuleva.mortgage.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
record AuthProperties(String mode, Duration stubDelay, StubPerson stubPerson) {

  record StubPerson(String personalCode, String firstName, String lastName) {}
}
