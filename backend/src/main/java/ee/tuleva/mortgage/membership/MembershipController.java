package ee.tuleva.mortgage.membership;

import ee.tuleva.mortgage.auth.CurrentBorrower;
import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Joining the pool requires Tuleva membership (D32); members do not pay the fee again. */
@RestController
@RequestMapping("/v1/me/membership")
@RequiredArgsConstructor
@EnableConfigurationProperties(MembershipProperties.class)
class MembershipController {

  private final MembershipChecker checker;
  private final BorrowerService borrowers;

  record Membership(boolean member) {}

  @GetMapping
  Membership membership(HttpSession session) {
    String personalCode =
        CurrentBorrower.id(session)
            .flatMap(borrowers::find)
            .map(Borrower::getPersonalCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    return new Membership(checker.isMember(personalCode));
  }
}
