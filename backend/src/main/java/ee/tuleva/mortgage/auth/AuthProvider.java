package ee.tuleva.mortgage.auth;

import org.jspecify.annotations.Nullable;

public interface AuthProvider {

  Challenge start(AuthMethod method, @Nullable String personalCode, @Nullable String phoneNumber);

  Outcome poll(String loginId);

  void cancel(String loginId);

  record Challenge(
      String loginId, @Nullable String verificationCode, @Nullable String deviceLink) {}

  record Person(String personalCode, String firstName, String lastName) {}

  record Outcome(Status status, @Nullable Person person) {
    static Outcome pending() {
      return new Outcome(Status.PENDING, null);
    }

    static Outcome complete(Person person) {
      return new Outcome(Status.COMPLETE, person);
    }

    static Outcome failed() {
      return new Outcome(Status.FAILED, null);
    }
  }

  enum Status {
    PENDING,
    COMPLETE,
    FAILED
  }
}
