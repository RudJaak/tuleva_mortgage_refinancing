package ee.tuleva.mortgage.commitment;

import static ee.tuleva.mortgage.commitment.CommitmentRejectedException.Reason.PHONE_NUMBER_INVALID;

import ee.tuleva.mortgage.auth.AuthMethod;
import ee.tuleva.mortgage.auth.AuthProvider;
import ee.tuleva.mortgage.auth.CurrentBorrower;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/me/commitment")
@RequiredArgsConstructor
class CommitmentController {

  private static final Pattern ESTONIAN_MOBILE = Pattern.compile("\\+372\\d{7,8}");

  private final CommitmentService commitments;

  @GetMapping
  CommitmentService.CommitmentView current(HttpSession session) {
    return commitments.current(borrowerId(session));
  }

  /**
   * Signs with the method of the login by default (and its Mobile-ID phone), so the borrower just
   * presses the button; a request can name another method.
   */
  @PostMapping("/signing")
  CommitmentService.SigningStart sign(
      @RequestBody(required = false) @Nullable SignRequest request, HttpSession session) {
    AuthMethod method =
        request != null && request.method() != null
            ? request.method()
            : CurrentBorrower.loginMethod(session).orElse(AuthMethod.SMART_ID);
    String phoneNumber =
        request != null && request.phoneNumber() != null
            ? request.phoneNumber()
            : CurrentBorrower.loginPhoneNumber(session).orElse(null);
    if (method == AuthMethod.MOBILE_ID
        && (phoneNumber == null || !ESTONIAN_MOBILE.matcher(phoneNumber).matches())) {
      throw new CommitmentRejectedException(PHONE_NUMBER_INVALID, "Phone number invalid");
    }
    return commitments.startSigning(borrowerId(session), method, phoneNumber);
  }

  @GetMapping("/signing/{signingId}")
  SigningStatus status(@PathVariable String signingId, HttpSession session) {
    return new SigningStatus(commitments.poll(borrowerId(session), signingId));
  }

  @DeleteMapping("/signing/{signingId}")
  ResponseEntity<Void> cancel(@PathVariable String signingId, HttpSession session) {
    commitments.cancel(borrowerId(session), signingId);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler
  ProblemDetail rejected(CommitmentRejectedException e) {
    var status =
        e.getReason() == CommitmentRejectedException.Reason.ALREADY_COMMITTED
            ? HttpStatus.CONFLICT
            : HttpStatus.UNPROCESSABLE_CONTENT;
    var problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
    problem.setProperty("reason", e.getReason());
    return problem;
  }

  private static UUID borrowerId(HttpSession session) {
    return CurrentBorrower.id(session)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  record SignRequest(@Nullable AuthMethod method, @Nullable String phoneNumber) {}

  record SigningStatus(AuthProvider.Status status) {}
}
