package ee.tuleva.mortgage.demo;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/demo")
@ConditionalOnBooleanProperty("app.demo.seed-enabled")
class DemoController {

  private final JdbcClient jdbc;

  DemoController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @PostMapping("/reset")
  @Transactional
  Map<String, Integer> reset() {
    int commitments =
        jdbc.sql(
                "delete from commitment where borrower_id in (select id from borrower where not synthetic)")
            .update();
    int borrowers = jdbc.sql("delete from borrower where not synthetic").update();
    return Map.of("removedCommitments", commitments, "removedBorrowers", borrowers);
  }
}
