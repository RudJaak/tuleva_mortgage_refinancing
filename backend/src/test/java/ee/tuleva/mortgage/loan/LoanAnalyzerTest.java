package ee.tuleva.mortgage.loan;

import static ee.tuleva.mortgage.loan.LoanSummary.Flag.BALANCE_UNDER_MINIMUM;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.BORROWER_NOT_A_PARTY;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.CO_BORROWERS;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.FIXED_RATE_PERIOD;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.MARGIN_AT_OR_BELOW_TARGET;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.MISSING_KEY_FIELDS;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.NOT_SIGNED_BY_BORROWER;
import static ee.tuleva.mortgage.loan.LoanSummary.Flag.REMAINING_TERM_UNDER_MINIMUM;
import static org.assertj.core.api.Assertions.assertThat;

import ee.tuleva.mortgage.borrower.PersonNames;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
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
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoanAnalyzerTest {

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

  private final ExtractedLoanContract stub =
      new StubLoanContractExtractor()
          .extract(new DocumentFile("c.pdf", DocumentFile.PDF, new byte[0]));

  @Test
  void derivesTheCurrentRateRemainingTermAndAnEstimatedBalance() {
    var summary = analyzer.analyze(stub, "Mari", "Maasikas", LocalDate.of(2000, 1, 1), null);

    // 6M Euribor 2.936 + margin 1.95
    assertThat(summary.currentInterestRate()).isEqualByComparingTo("4.886");
    assertThat(summary.rateBasis()).isEqualTo(RateBasis.EURIBOR_PLUS_MARGIN);
    assertThat(summary.maturityDate()).isEqualTo(LocalDate.of(2049, 6, 14));
    assertThat(summary.remainingMonths()).isEqualTo(271);
    // present value of 271 instalments of 920 at 4.886% a year
    assertThat(summary.balance()).isEqualByComparingTo("150828");
    assertThat(summary.balanceSource()).isEqualTo(BalanceSource.ESTIMATED);
    assertThat(summary.ageAtMaturity()).isEqualTo(49);
    assertThat(summary.partyCheck()).isEqualTo(PersonNames.Match.NOT_CHECKED);
    assertThat(summary.missingFields()).isEmpty();
    assertThat(summary.flags()).isEmpty();
  }

  @Test
  void aStatedBalanceWinsOverTheEstimate() {
    var contract = with(stub, new BigDecimal("142000.00"));

    var summary = analyzer.analyze(contract, null, null, null, null);

    assertThat(summary.balance()).isEqualByComparingTo("142000.00");
    assertThat(summary.balanceSource()).isEqualTo(BalanceSource.CONTRACT);
  }

  @Test
  void flagsWhatKeepsALoanOutOfThePool() {
    var contract =
        new ExtractedLoanContract(
            "LHV Pank AS",
            null,
            LocalDate.of(2012, 3, 1),
            List.of("Jaan Tamm", "Mari Tamm"),
            new BigDecimal("80000"),
            "EUR",
            "6 kuu EURIBOR",
            new BigDecimal("1.10"),
            RateType.MIXED,
            LocalDate.of(2028, 3, 1),
            new BigDecimal("3.90"),
            LocalDate.of(2032, 3, 1),
            240,
            new BigDecimal("420.00"),
            1,
            RepaymentType.ANNUITY,
            null,
            null,
            null,
            null,
            null,
            Map.of());

    var summary = analyzer.analyze(contract, "Kati", "Kask", null, false);

    assertThat(summary.rateBasis()).isEqualTo(RateBasis.FIXED);
    assertThat(summary.currentInterestRate()).isEqualByComparingTo("3.90");
    assertThat(summary.borrowerCount()).isEqualTo(2);
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
    var contract =
        new ExtractedLoanContract(
            null,
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            RateType.UNKNOWN,
            null,
            null,
            null,
            null,
            null,
            null,
            RepaymentType.UNKNOWN,
            null,
            null,
            null,
            null,
            null,
            Map.of());

    var summary = analyzer.analyze(contract, null, null, null, null);

    assertThat(summary.missingFields())
        .containsExactly(LoanField.LENDER, LoanField.MARGIN, LoanField.MATURITY_DATE);
    assertThat(summary.flags()).containsExactly(MISSING_KEY_FIELDS);
    assertThat(summary.balanceSource()).isEqualTo(BalanceSource.UNKNOWN);
  }

  @Test
  void anEqualPrincipalLoanFallsLinearly() {
    var contract =
        new ExtractedLoanContract(
            "SEB",
            null,
            LocalDate.of(2016, 10, 15),
            List.of(),
            new BigDecimal("120000"),
            "EUR",
            null,
            new BigDecimal("1.5"),
            RateType.VARIABLE,
            null,
            null,
            LocalDate.of(2046, 10, 15),
            360,
            null,
            null,
            RepaymentType.EQUAL_PRINCIPAL,
            null,
            null,
            null,
            null,
            null,
            Map.of());

    // 240 of 360 months left
    assertThat(analyzer.analyze(contract, null, null, null, null).balance())
        .isEqualByComparingTo("80000");
  }

  @Test
  void theBorrowerIsAPartyWhenAnyBorrowerNameMatches() {
    var contract =
        new ExtractedLoanContract(
            "SEB",
            null,
            null,
            List.of("TAMM, JAAN", "Mari Õunapuu"),
            null,
            null,
            null,
            new BigDecimal("1.5"),
            RateType.VARIABLE,
            null,
            null,
            LocalDate.of(2050, 1, 1),
            null,
            null,
            null,
            RepaymentType.UNKNOWN,
            null,
            null,
            null,
            null,
            null,
            Map.of());

    var summary = analyzer.analyze(contract, "Mari", "Ounapuu", null, true);

    assertThat(summary.partyCheck()).isEqualTo(PersonNames.Match.MATCH);
    assertThat(summary.flags()).containsExactly(CO_BORROWERS);
  }

  private static ExtractedLoanContract with(ExtractedLoanContract c, BigDecimal balance) {
    return new ExtractedLoanContract(
        c.lender(),
        c.contractNumber(),
        c.contractDate(),
        c.borrowers(),
        c.principal(),
        c.currency(),
        c.baseRate(),
        c.margin(),
        c.rateType(),
        c.fixedRateUntil(),
        c.statedInterestRate(),
        c.maturityDate(),
        c.termMonths(),
        c.monthlyPayment(),
        c.paymentDay(),
        c.repaymentType(),
        c.mortgageAmount(),
        c.collateral(),
        c.earlyRepaymentTerms(),
        balance,
        LocalDate.of(2026, 10, 1),
        c.evidence());
  }
}
