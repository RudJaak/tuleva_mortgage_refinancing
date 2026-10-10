package ee.tuleva.mortgage.auth;

import org.jspecify.annotations.Nullable;

public interface AuthProvider {

  Challenge start(AuthMethod method, String personalCode, @Nullable String phoneNumber);

  Outcome poll(String loginId);

  void cancel(String loginId);

  /** {@code firstName} is known from the start when the name directory has the person. */
  record Challenge(String loginId, String verificationCode, @Nullable String firstName) {}

  /** Names are null when the person is not in the name directory. */
  record Person(String personalCode, @Nullable String firstName, @Nullable String lastName) {}

  record Outcome(Status status, @Nullable Person person) {
    static Outcome pending() {
      return new Outcome(Status.PENDING, null);
    }

    static Outcome verifying() {
      return new Outcome(Status.VERIFYING, null);
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
    /** Confirmed on the device, the login is being completed (the stub emulates this step). */
    VERIFYING,
    COMPLETE,
    FAILED
  }
}
