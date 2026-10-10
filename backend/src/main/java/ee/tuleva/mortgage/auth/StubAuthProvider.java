package ee.tuleva.mortgage.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Emulates a Smart-ID or Mobile-ID login: shows a verification code while the person would confirm
 * on their phone, then a short verifying step. Nothing is checked; the name comes from the public
 * eID certificate directories, so the demo greets people by their real name.
 */
@RequiredArgsConstructor
class StubAuthProvider implements AuthProvider {

  private final AuthProperties properties;
  private final NameDirectory names;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, PendingLogin> logins = new ConcurrentHashMap<>();

  @Override
  public Challenge start(AuthMethod method, String personalCode, @Nullable String phoneNumber) {
    String loginId = UUID.randomUUID().toString();
    var name = names.find(personalCode);
    var person =
        new Person(
            personalCode,
            name.map(PersonName::firstName).orElse(null),
            name.map(PersonName::lastName).orElse(null));
    logins.put(loginId, new PendingLogin(person, clock.instant()));
    return new Challenge(loginId, verificationCode(), person.firstName());
  }

  @Override
  public Outcome poll(String loginId) {
    PendingLogin login = logins.get(loginId);
    if (login == null) {
      return Outcome.failed();
    }
    Instant confirmedOnPhone = login.startedAt().plus(properties.stubCodeDisplay());
    Instant now = clock.instant();
    if (now.isBefore(confirmedOnPhone)) {
      return Outcome.pending();
    }
    if (now.isBefore(confirmedOnPhone.plus(properties.stubVerifying()))) {
      return Outcome.verifying();
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
