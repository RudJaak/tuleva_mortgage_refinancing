package ee.tuleva.mortgage.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class StubAuthProviderTest {

  private Instant now = Instant.parse("2026-10-10T10:00:00Z");
  private final Clock clock =
      new Clock() {
        @Override
        public ZoneOffset getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return now;
        }
      };

  private final StubAuthProvider provider =
      new StubAuthProvider(
          new AuthProperties(
              "stub",
              Duration.ofSeconds(2),
              Duration.ofSeconds(10),
              new AuthProperties.StubPerson("40504040001", "Mari", "Maasikas")),
          clock);

  @Test
  void mobileIdShowsTheCodeThenVerifiesThenLogsIn() {
    var challenge = provider.start(AuthMethod.MOBILE_ID, "60001017869", "+37268000769");

    now = now.plusSeconds(9);
    assertThat(provider.poll(challenge.loginId()).status()).isEqualTo(AuthProvider.Status.PENDING);

    now = now.plusSeconds(1);
    assertThat(provider.poll(challenge.loginId()).status())
        .isEqualTo(AuthProvider.Status.VERIFYING);

    now = now.plusSeconds(2);
    var outcome = provider.poll(challenge.loginId());
    assertThat(outcome.status()).isEqualTo(AuthProvider.Status.COMPLETE);
    assertThat(outcome.person().personalCode()).isEqualTo("60001017869");
    assertThat(outcome.person().firstName()).isEqualTo("Mari");
  }

  @Test
  void showsAVerificationCodeForMobileIdAndADeviceLinkForSmartId() {
    assertThat(
            provider.start(AuthMethod.MOBILE_ID, "60001017869", "+37268000769").verificationCode())
        .matches("\\d{4}");
    assertThat(provider.start(AuthMethod.SMART_ID, null, null).deviceLink()).startsWith("https://");
  }

  @Test
  void logsInTheStubPersonWhenNoPersonalCodeIsGiven() {
    var challenge = provider.start(AuthMethod.ID_CARD, null, null);
    now = now.plusSeconds(2);

    assertThat(provider.poll(challenge.loginId()).person().personalCode()).isEqualTo("40504040001");
  }

  @Test
  void aCancelledOrCompletedLoginCannotBePolledAgain() {
    var cancelled = provider.start(AuthMethod.SMART_ID, null, null);
    provider.cancel(cancelled.loginId());
    assertThat(provider.poll(cancelled.loginId()).status()).isEqualTo(AuthProvider.Status.FAILED);

    var completed = provider.start(AuthMethod.SMART_ID, null, null);
    now = now.plusSeconds(2);
    provider.poll(completed.loginId());
    assertThat(provider.poll(completed.loginId()).status()).isEqualTo(AuthProvider.Status.FAILED);
  }
}
