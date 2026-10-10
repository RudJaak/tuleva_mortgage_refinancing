package ee.tuleva.mortgage.borrower;

import static org.assertj.core.api.Assertions.assertThat;

import ee.tuleva.mortgage.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BorrowerServiceTest {

  @Autowired private BorrowerRepository borrowers;

  private BorrowerService service() {
    return new BorrowerService(
        borrowers, Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void createsABorrowerOnTheFirstLogin() {
    var borrower = service().findOrCreate("60001017869", "Rudolf", "");

    assertThat(borrowers.findByPersonalCode("60001017869")).contains(borrower);
    assertThat(borrower.getFirstName()).isEqualTo("Rudolf");
  }

  @Test
  void keepsTheSameBorrowerAndTakesTheLatestNameOnLaterLogins() {
    var first = service().findOrCreate("60001017869", "Mari", "Maasikas");

    var again = service().findOrCreate("60001017869", "Rudolf", "");

    assertThat(again.getId()).isEqualTo(first.getId());
    assertThat(again.getFirstName()).isEqualTo("Rudolf");
    assertThat(borrowers.count()).isEqualTo(1);
  }
}
