package ee.tuleva.mortgage.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
class StubAuthProvider implements AuthProvider {

  private final AuthProperties properties;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, PendingLogin> logins = new ConcurrentHashMap<>();

  @Override
  public Challenge start(
      AuthMethod method, @Nullable String personalCode, @Nullable String phoneNumber) {
    String loginId = UUID.randomUUID().toString();
    var stubPerson = properties.stubPerson();
    var person =
        new Person(
            personalCode != null ? personalCode : stubPerson.personalCode(),
            stubPerson.firstName(),
            stubPerson.lastName());
    logins.put(loginId, new PendingLogin(person, clock.instant()));
    return switch (method) {
      case MOBILE_ID -> new Challenge(loginId, verificationCode(), null);
      case SMART_ID -> new Challenge(loginId, null, "https://smart-id.com/demo?login=" + loginId);
      case ID_CARD -> new Challenge(loginId, null, null);
    };
  }

  @Override
  public Outcome poll(String loginId) {
    PendingLogin login = logins.get(loginId);
    if (login == null) {
      return Outcome.failed();
    }
    Instant confirmedAt = login.startedAt().plus(properties.stubDelay());
    if (clock.instant().isBefore(confirmedAt)) {
      return Outcome.pending();
    }
    logins.remove(loginId);
    return Outcome.complete(login.person());
  }

  @Override
  public void cancel(String loginId) {
    logins.remove(loginId);
  }

  private String verificationCode() {
    return String.format("%04d", random.nextInt(10_000));
  }

  private record PendingLogin(Person person, Instant startedAt) {}
}
