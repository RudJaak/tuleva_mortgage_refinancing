package ee.tuleva.mortgage.statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.loan.LoanParties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class StatementsOverviewServiceTest {

  private static final UUID BORROWER = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

  private final StoredStatementSummaryRepository summaries =
      mock(StoredStatementSummaryRepository.class);
  private final LoanParties loanParties = mock(LoanParties.class);
  private final BorrowerService borrowers = mock(BorrowerService.class);
  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final StatementsOverviewService service =
      new StatementsOverviewService(summaries, loanParties, borrowers, jsonMapper);

  // the synthetic statement: 2480 income and 1001.40 obligations a month
  private final StatementSummary stub =
      new StatementAnalyzer(
              Clock.fixed(NOW, ZoneId.of("Europe/Tallinn")), new StatementProperties(true))
          .analyze(
              new StubStatementExtractor()
                  .extract(new DocumentFile("s.pdf", DocumentFile.PDF, new byte[0])),
              null,
              null);

  @Test
  void groupsAccountsPerBorrowerAndListsWhoIsStillMissing() {
    given(loanParties.borrowerNames(BORROWER)).willReturn(List.of("Jaan Tamm", "Mari Tamm"));
    given(summaries.findByBorrowerIdOrderByCreatedAtDesc(BORROWER))
        .willReturn(
            List.of(
                stored("TAMM, JAAN", "EE…1111"),
                stored("JAAN TAMM", "EE…2222"),
                stored("Jaan Tamm", "EE…1111")));

    var overview = service.overview(BORROWER);

    assertThat(overview.persons())
        .extracting(p -> p.name())
        .containsExactly("Jaan Tamm", "Mari Tamm");
    var jaan = overview.persons().getFirst();
    assertThat(jaan.documentIds()).as("EE…1111 counted once").hasSize(2);
    assertThat(jaan.totals().monthlyIncome()).isEqualByComparingTo("4960.00");
    assertThat(jaan.onLoan()).isTrue();
    assertThat(overview.persons().get(1).documentIds()).isEmpty();
    assertThat(overview.household().monthlyIncome()).isEqualByComparingTo("4960.00");
    assertThat(overview.household().monthlyObligations()).isEqualByComparingTo("2002.80");
    assertThat(overview.household().debtServiceRatio()).isEqualByComparingTo("0.40");
  }

  @Test
  void withoutAContractTheLoggedInBorrowerIsExpectedAndUnnamedStatementsAreTheirs() {
    given(loanParties.borrowerNames(BORROWER)).willReturn(List.of());
    given(borrowers.find(BORROWER))
        .willReturn(Optional.of(Borrower.identified("60001017869", "Mari", "Maasikas", NOW)));
    given(summaries.findByBorrowerIdOrderByCreatedAtDesc(BORROWER))
        .willReturn(List.of(stored(null, null)));

    var overview = service.overview(BORROWER);

    assertThat(overview.persons())
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.name()).isEqualTo("Mari Maasikas");
              assertThat(p.documentIds()).hasSize(1);
            });
  }

  @Test
  void aHolderWhoIsNotOnTheLoanIsListedSeparately() {
    given(loanParties.borrowerNames(BORROWER)).willReturn(List.of("Jaan Tamm"));
    given(summaries.findByBorrowerIdOrderByCreatedAtDesc(BORROWER))
        .willReturn(List.of(stored("Kati Kask", "EE…3333")));

    var overview = service.overview(BORROWER);

    assertThat(overview.persons())
        .extracting(p -> p.name() + "/" + p.onLoan())
        .containsExactly("Jaan Tamm/true", "Kati Kask/false");
  }

  private StoredStatementSummary stored(String holder, String account) {
    return StoredStatementSummary.of(
        UUID.randomUUID(),
        BORROWER,
        stub,
        holder,
        account,
        jsonMapper.writeValueAsString(stub),
        NOW);
  }
}
