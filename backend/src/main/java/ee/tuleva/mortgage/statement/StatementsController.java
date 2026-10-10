package ee.tuleva.mortgage.statement;

import ee.tuleva.mortgage.auth.CurrentBorrower;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/me/statements")
@RequiredArgsConstructor
class StatementsController {

  private final StatementsOverviewService overviewService;

  @GetMapping
  StatementsOverview overview(HttpSession session) {
    return overviewService.overview(
        CurrentBorrower.id(session)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
  }
}
