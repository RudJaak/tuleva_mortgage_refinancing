package ee.tuleva.mortgage.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
/**
 * @param stubDelay how long a stub login takes; for Mobile-ID, the "verifying" step after the code
 * @param stubMobileIdCode how long the stub shows the Mobile-ID verification code, as a person
 *     would take to compare it with their phone and enter their PIN
 */
record AuthProperties(
    String mode, Duration stubDelay, Duration stubMobileIdCode, StubPerson stubPerson) {

  record StubPerson(String personalCode, String firstName, String lastName) {}
}
