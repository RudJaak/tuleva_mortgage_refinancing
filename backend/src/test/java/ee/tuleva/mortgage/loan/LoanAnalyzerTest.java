package ee.tuleva.mortgage.loan;

import static ee.tuleva.mortgage.loan.LoanSummary.Flag.BALANCE_UNDER_MINIMUM;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.BORROWER_NOT_A_PARTY;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.CO_BORROWERS;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.FIXED_RATE_PERIOD;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.MARGIN_AT_OR_BELOW_TARGET;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.MISSING_KEY_FIELDS;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.NOT_SIGNED_BY_BORROWER;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.REMAINING_TERM_UNDER_MINIMUM;
import static ee.tuleva.mortgage.loan.TestContracts.contract;
import static org.assertj.core.api.Assertions.assertThat;

import ee.tuleva.mortgage.borrower.PersonNames;
import ee.tuleva.mortgage.loan.LoanSummary.BalanceSource;
import ee.tuleva.mortgage.loan.LoanSummary.RateBasis;
import ee.tuleva.mortgage.pool.CalculatorProperties;
import ee.tuleva.mortgage.pool.PoolProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoanAnalyzerTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

  private final LoanAnalyzer analyzer =
      new LoanAnalyzer(
          Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Tallinn")),
          new CalculatorProperties(
              new BigDecimal("2.936"),
              LocalDate.of(2026, 10, 8),
              new BigDecimal("1.70"),
              new BigDecimal("1.60"),
              new BigDecimal("400"),
              new BigDecimal("3")),
          new PoolProperties(
              OffsetDateTime.parse("2026-12-31T23:59:59+02:00"),
              new BigDecimal("100000000"),
              List.of(
                  new PoolProperties.Milestone(new BigDecimal("300000000"), new BigDecimal("1.00")),
                  new PoolProperties.Milestone(
                      new BigDecimal("100000000"), new BigDecimal("1.20"))),
              10));

  @Test
  void summarisesTheSyntheticContract() {
    var stub = new StubLoanContractExtractor().extract(List.of());

    var summary = analyzer.analyze(stub, "Mari", "Maasikas", LocalDate.of(2000, 1, 1), null);

    // 6M Euribor 2.936 + margin 1.95
    assertThat(summary.currentInterestRate()).isEqualByComparingTo("4.886");
    assertThat(summary.rateBasis()).isEqualTo(RateBasis.EURIBOR_PLUS_MARGIN);
    assertThat(summary.maturityDate()).isEqualTo(LocalDate.of(2049, 6, 14));
    assertThat(summary.remainingMonths()).isEqualTo(271);
    // 185 000 over 360 months at the 4.34% that a 920 instalment implies, 88 months paid
    assertThat(summary.balance()).isEqualByComparingTo("159075");
    assertThat(summary.balanceSource()).isEqualTo(BalanceSource.ESTIMATED);
    assertThat(summary.loanToValue()).isEqualByComparingTo("0.69");
    assertThat(summary.ageAtMaturity()).isEqualTo(49);
    assertThat(summary.partyCheck()).isEqualTo(PersonNames.Match.NOT_CHECKED);
    assertThat(summary.flags()).isEmpty();
  }

  @Test
  void theBalanceFollowsTheOriginalInstalmentNotTodaysRate() {
    // Signed when Euribor was 0: the 393.14 instalment implies about 2.99%, not today's 5.9%
    var signedIn2020 =
        contract()
            .signed("2020-03-05")
            .principal("82990")
            .margin("2.95")
            .term(300)
            .maturity("2045-03-05")
            .payment("393.14")
            .build();

    var summary = analyzer.analyze(signedIn2020, null, null, null, null);

    assertThat(summary.balance()).isEqualByComparingTo("66744");
    assertThat(summary.currentInterestRate()).isEqualByComparingTo("5.886");
  }

  @Test
  void withoutAnInstalmentTheBalanceAmortisesAtTheMargin() {
    var noInstalment =
        contract().signed("2020-03-05").principal("82990").margin("2.95").maturity("2045-03-05");

    // 300 months derived from the dates, 79 paid at 2.95%
    assertThat(analyzer.analyze(noInstalment.build(), null, null, null, null).balance())
        .isEqualByComparingTo(
            LoanAnalyzer.estimateBalance(
                noInstalment.term(300).build(), LocalDate.of(2045, 3, 5), TODAY));
  }

  @Test
  void aStatedBalanceWinsOverTheEstimate() {
    var withBalance =
        contract()
            .signed("2019-06-14")
            .principal("185000")
            .margin("1.95")
            .maturity("2049-06-14")
            .balance("142000.00")
            .build();

    var summary = analyzer.analyze(withBalance, null, null, null, null);

    assertThat(summary.balance()).isEqualByComparingTo("142000.00");
    assertThat(summary.balanceSource()).isEqualTo(BalanceSource.CONTRACT);
  }

  @Test
  void flagsWhatKeepsALoanOutOfThePool() {
    var weak =
        contract()
            .lender("LHV Pank AS")
            .signed("2012-03-01")
            .borrowers("Jaan Tamm", "Mari Tamm")
            .principal("80000")
            .margin("1.10")
            .fixedUntil("2028-03-01", "3.90")
            .maturity("2032-03-01")
            .term(240)
            .payment("420.00")
            .build();

    var summary = analyzer.analyze(weak, "Kati", "Kask", LocalDate.of(1980, 1, 1), false);

    assertThat(summary.rateBasis()).isEqualTo(RateBasis.FIXED);
    assertThat(summary.currentInterestRate()).isEqualByComparingTo("3.90");
    assertThat(summary.borrowerCount()).isEqualTo(2);
    assertThat(summary.ageAtMaturity()).as("not the logged-in borrower's contract").isNull();
    assertThat(summary.flags())
        .containsExactly(
            FIXED_RATE_PERIOD,
            MARGIN_AT_OR_BELOW_TARGET,
            REMAINING_TERM_UNDER_MINIMUM,
            BALANCE_UNDER_MINIMUM,
            CO_BORROWERS,
            BORROWER_NOT_A_PARTY,
            NOT_SIGNED_BY_BORROWER);
  }

  @Test
  void aContractWithoutMarginOrMaturityCannotBeScreened() {
    var unreadable = contract().lender(null).unknownRate().build();

    var summary = analyzer.analyze(unreadable, null, null, null, null);

    assertThat(summary.missingFields())
        .containsExactly(LoanField.LENDER, LoanField.MARGIN, LoanField.MATURITY_DATE);
    assertThat(summary.flags()).containsExactly(MISSING_KEY_FIELDS);
    assertThat(summary.balanceSource()).isEqualTo(BalanceSource.UNKNOWN);
  }

  @Test
  void anEqualPrincipalLoanFallsLinearly() {
    var linear =
        contract()
            .signed("2016-10-15")
            .principal("120000")
            .margin("1.5")
            .maturity("2046-10-15")
            .term(360)
            .equalPrincipal()
            .build();

    // 240 of 360 months left
    assertThat(analyzer.analyze(linear, null, null, null, null).balance())
        .isEqualByComparingTo("80000");
  }

  @Test
  void theBorrowerIsAPartyWhenAnyBorrowerNameMatches() {
    var shared =
        contract()
            .borrowers("TAMM, JAAN", "Mari Õunapuu")
            .margin("1.5")
            .maturity("2050-01-01")
            .build();

    var summary = analyzer.analyze(shared, "Mari", "Ounapuu", LocalDate.of(1990, 5, 1), true);

    assertThat(summary.partyCheck()).isEqualTo(PersonNames.Match.MATCH);
    assertThat(summary.ageAtMaturity()).isEqualTo(59);
    assertThat(summary.flags()).containsExactly(CO_BORROWERS);
  }

  @Test
  void theImpliedRateRecoversTheAnnuityRate() {
    double monthly = LoanAnalyzer.impliedMonthlyRate(82990, 300, 393.14);

    assertThat(monthly * 1200).isBetween(2.98, 3.0);
    assertThat(LoanAnalyzer.impliedMonthlyRate(1200, 12, 100)).isZero();
  }
}
