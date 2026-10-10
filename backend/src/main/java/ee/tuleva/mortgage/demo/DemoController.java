package ee.tuleva.mortgage.demo;

import ee.tuleva.mortgage.auth.CurrentBorrower;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/demo")
@ConditionalOnBooleanProperty("app.demo.seed-enabled")
@RequiredArgsConstructor
class DemoController {

  private final JdbcClient jdbc;
  private final DemoFlowReset flowReset;

  /** Clears the logged-in borrower's documents and loan, so the flow can be shown again. */
  @PostMapping("/flow/reset")
  ResponseEntity<Void> resetFlow(HttpSession session) {
    var borrowerId =
        CurrentBorrower.id(session)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    flowReset.reset(borrowerId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reset")
  @Transactional
  Map<String, Integer> reset() {
    jdbc.sql("select id from borrower where not synthetic")
        .query(UUID.class)
        .list()
        .forEach(flowReset::reset);
    int commitments =
        jdbc.sql(
                "delete from commitment where borrower_id in (select id from borrower where not synthetic)")
            .update();
    int borrowers = jdbc.sql("delete from borrower where not synthetic").update();
    return Map.of("removedCommitments", commitments, "removedBorrowers", borrowers);
  }
}
