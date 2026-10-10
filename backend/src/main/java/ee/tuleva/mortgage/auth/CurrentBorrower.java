package ee.tuleva.mortgage.auth;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;

/** The borrower logged in on this session, for controllers in other modules. */
public final class CurrentBorrower {

  private CurrentBorrower() {}

  public static Optional<UUID> id(HttpSession session) {
    return session.getAttribute(AuthController.BORROWER_ID) instanceof UUID id
        ? Optional.of(id)
        : Optional.empty();
  }

  /** How the borrower logged in on this session. */
  public static Optional<AuthMethod> loginMethod(HttpSession session) {
    return session.getAttribute(AuthController.LOGIN_METHOD) instanceof AuthMethod method
        ? Optional.of(method)
        : Optional.empty();
  }

  /** The phone number of a Mobile-ID login on this session. */
  public static Optional<String> loginPhoneNumber(HttpSession session) {
    return session.getAttribute(AuthController.LOGIN_PHONE) instanceof String phone
        ? Optional.of(phone)
        : Optional.empty();
  }
}
