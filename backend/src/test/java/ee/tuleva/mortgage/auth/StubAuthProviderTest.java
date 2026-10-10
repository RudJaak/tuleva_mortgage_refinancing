package ee.tuleva.mortgage.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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

  private final NameDirectory names =
      code ->
          code.equals("60001017869")
              ? Optional.of(new PersonName("Mari Liis", "Õunapuu"))
              : Optional.empty();

  private final StubAuthProvider provider =
      new StubAuthProvider(
          new AuthProperties(
              Duration.ofSeconds(10),
              Duration.ofSeconds(2),
              new AuthProperties.NameLookup(
                  false, Duration.ofSeconds(3), 30, Duration.ofMinutes(1), List.of())),
          names,
          clock);

  @Test
  void showsTheCodeThenVerifiesThenLogsInTheNamedPerson() {
    var challenge = provider.start(AuthMethod.MOBILE_ID, "60001017869", "+37268000769");

    assertThat(challenge.verificationCode()).matches("\\d{4}");
    assertThat(challenge.firstName()).isEqualTo("Mari Liis");

    now = now.plusSeconds(9);
    assertThat(provider.poll(challenge.loginId()).status()).isEqualTo(AuthProvider.Status.PENDING);

    now = now.plusSeconds(1);
    assertThat(provider.poll(challenge.loginId()).status())
        .isEqualTo(AuthProvider.Status.VERIFYING);

    now = now.plusSeconds(2);
    var outcome = provider.poll(challenge.loginId());
    assertThat(outcome.status()).isEqualTo(AuthProvider.Status.COMPLETE);
    assertThat(outcome.person().personalCode()).isEqualTo("60001017869");
    assertThat(outcome.person().firstName()).isEqualTo("Mari Liis");
    assertThat(outcome.person().lastName()).isEqualTo("Õunapuu");
  }

  @Test
  void smartIdTakesTheSameSteps() {
    var challenge = provider.start(AuthMethod.SMART_ID, "60001017869", null);

    now = now.plusSeconds(11);
    assertThat(provider.poll(challenge.loginId()).status())
        .isEqualTo(AuthProvider.Status.VERIFYING);
  }

  @Test
  void someoneNotInTheDirectoryLogsInWithoutAName() {
    var challenge = provider.start(AuthMethod.SMART_ID, "39901012239", null);
    now = now.plusSeconds(12);

    var person = provider.poll(challenge.loginId()).person();
    assertThat(challenge.firstName()).isNull();
    assertThat(person.firstName()).isNull();
    assertThat(person.lastName()).isNull();
  }

  @Test
  void aCancelledOrCompletedLoginCannotBePolledAgain() {
    var cancelled = provider.start(AuthMethod.SMART_ID, "60001017869", null);
    provider.cancel(cancelled.loginId());
    assertThat(provider.poll(cancelled.loginId()).status()).isEqualTo(AuthProvider.Status.FAILED);

    var completed = provider.start(AuthMethod.SMART_ID, "60001017869", null);
    now = now.plusSeconds(12);
    provider.poll(completed.loginId());
    assertThat(provider.poll(completed.loginId()).status()).isEqualTo(AuthProvider.Status.FAILED);
  }
}
