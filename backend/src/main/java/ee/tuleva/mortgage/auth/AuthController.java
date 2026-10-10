package ee.tuleva.mortgage.auth;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import ee.tuleva.mortgage.borrower.PersonalCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
class AuthController {

  static final String BORROWER_ID = "borrowerId";
  private static final String PENDING_LOGIN = "pendingLogin";
  private static final Pattern ESTONIAN_MOBILE = Pattern.compile("\\+372\\d{7,8}");

  private final AuthProvider authProvider;
  private final BorrowerService borrowerService;
  private final ApplicationEventPublisher events;

  @PostMapping("/auth/{method}")
  StartResponse start(
      @PathVariable String method, @RequestBody StartRequest request, HttpSession session) {
    AuthMethod authMethod = parse(method);
    if (authMethod == AuthMethod.MOBILE_ID) {
      requireValid(request);
    }
    var challenge = authProvider.start(authMethod, request.personalCode(), request.phoneNumber());
    session.setAttribute(PENDING_LOGIN, challenge.loginId());
    return new StartResponse(
        challenge.loginId(), challenge.verificationCode(), challenge.deviceLink());
  }

  @GetMapping("/auth/logins/{loginId}")
  PollResponse poll(@PathVariable String loginId, HttpServletRequest request, HttpSession session) {
    requireOwnLogin(loginId, session);
    var outcome = authProvider.poll(loginId);
    if (outcome.status() != AuthProvider.Status.COMPLETE || outcome.person() == null) {
      return new PollResponse(outcome.status(), null);
    }
    var person = outcome.person();
    Borrower borrower =
        borrowerService.findOrCreate(person.personalCode(), person.firstName(), person.lastName());
    request.changeSessionId();
    session.removeAttribute(PENDING_LOGIN);
    session.setAttribute(BORROWER_ID, borrower.getId());
    events.publishEvent(new BorrowerLoggedIn(borrower.getId()));
    return new PollResponse(outcome.status(), person.firstName());
  }

  @DeleteMapping("/auth/logins/{loginId}")
  ResponseEntity<Void> cancel(@PathVariable String loginId, HttpSession session) {
    requireOwnLogin(loginId, session);
    authProvider.cancel(loginId);
    session.removeAttribute(PENDING_LOGIN);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  ResponseEntity<MeResponse> me(HttpSession session) {
    if (!(session.getAttribute(BORROWER_ID) instanceof UUID borrowerId)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return borrowerService
        .find(borrowerId)
        .map(b -> ResponseEntity.ok(new MeResponse(b.getFirstName(), b.getLastName())))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @PostMapping("/auth/logout")
  ResponseEntity<Void> logout(HttpSession session) {
    session.invalidate();
    return ResponseEntity.noContent().build();
  }

  private static AuthMethod parse(String method) {
    return switch (method) {
      case "smart-id" -> AuthMethod.SMART_ID;
      case "mobile-id" -> AuthMethod.MOBILE_ID;
      case "id-card" -> AuthMethod.ID_CARD;
      default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    };
  }

  private static void requireValid(StartRequest request) {
    if (request.personalCode() == null || !PersonalCode.isValid(request.personalCode())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personal code invalid");
    }
    if (request.phoneNumber() == null
        || !ESTONIAN_MOBILE.matcher(request.phoneNumber()).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone number invalid");
    }
  }

  private static void requireOwnLogin(String loginId, HttpSession session) {
    if (!loginId.equals(session.getAttribute(PENDING_LOGIN))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  record StartRequest(@Nullable String personalCode, @Nullable String phoneNumber) {}

  record StartResponse(
      String loginId, @Nullable String verificationCode, @Nullable String deviceLink) {}

  record PollResponse(AuthProvider.Status status, @Nullable String firstName) {}

  record MeResponse(@Nullable String firstName, @Nullable String lastName) {}
}
