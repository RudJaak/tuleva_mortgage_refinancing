package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.auth.CurrentBorrower;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/me/loan")
@RequiredArgsConstructor
class LoanController {

  private final LoanService loanService;

  @GetMapping
  ResponseEntity<LoanService.LoanView> current(HttpSession session) {
    return ResponseEntity.of(loanService.current(borrowerId(session)));
  }

  /** The borrower confirms or corrects the fields read from their contract. */
  @PutMapping
  LoanService.LoanView confirm(@RequestBody LoanTerms terms, HttpSession session) {
    return loanService.confirm(borrowerId(session), terms);
  }

  private static UUID borrowerId(HttpSession session) {
    return CurrentBorrower.id(session)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }
}
