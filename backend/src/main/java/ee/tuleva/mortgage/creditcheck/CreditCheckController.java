package ee.tuleva.mortgage.creditcheck;

import ee.tuleva.mortgage.auth.CurrentBorrower;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/me/credit-check")
@RequiredArgsConstructor
class CreditCheckController {

  private final CreditChecks checks;

  record Consent(boolean consent) {}

  @GetMapping
  ResponseEntity<CreditChecks.View> current(HttpSession session) {
    return ResponseEntity.of(checks.current(borrowerId(session)));
  }

  /** Runs the check; the borrower must consent explicitly each time. */
  @PostMapping
  CreditChecks.View run(@RequestBody Consent request, HttpSession session) {
    if (!request.consent()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consent is required");
    }
    return checks.run(borrowerId(session));
  }

  private static UUID borrowerId(HttpSession session) {
    return CurrentBorrower.id(session)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }
}
