package ee.tuleva.mortgage.statement;

import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.DEBT_COLLECTION;
import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.HOLDER_MISMATCH;
import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.IRREGULAR_SALARY;
import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.LATE_FEES;
import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.NEGATIVE_BALANCE;
import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.NO_SALARY;
import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.SHORT_PERIOD;
import static ee.tuleva.mortgage.statement.StatementSummary.FlagCode.STALE_STATEMENT;
import static org.assertj.core.api.Assertions.assertThat;

import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.statement.ExtractedStatement.Transaction;
import ee.tuleva.mortgage.statement.StatementSummary.Flag;
import ee.tuleva.mortgage.statement.StatementSummary.HolderCheck;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatementAnalyzerTest {

  private final StatementAnalyzer analyzer =
      new StatementAnalyzer(
          Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Tallinn")),
          new StatementProperties(true));
  private final ExtractedStatement stub =
      new StubStatementExtractor()
          .extract(new DocumentFile("s.pdf", DocumentFile.PDF, new byte[0]));

  @Test
  void summarisesTheSyntheticStatement() {
    var summary = analyzer.analyze(stub, null, null);

    assertThat(summary.monthsCovered()).isEqualByComparingTo("6.0");
    assertThat(summary.analysedFrom()).isEqualTo(summary.periodStart());
    assertThat(summary.monthsAnalysed()).isEqualByComparingTo("6.0");
    assertThat(summary.income().oneOffIncome()).isEmpty();
    // 5 × 2410 + 2350 over 6 months
    assertThat(summary.income().averageMonthlySalary()).isEqualByComparingTo("2400.00");
    assertThat(summary.income().lowestMonthlySalary()).isEqualByComparingTo("2350.00");
    assertThat(summary.income().monthsWithSalary()).isEqualTo(6);
    assertThat(summary.income().employers()).isEqualTo(1);
    assertThat(summary.income().averageMonthlyOtherIncome()).isEqualByComparingTo("80.00");
    assertThat(summary.income().averageMonthlyTotal()).isEqualByComparingTo("2480.00");
    assertThat(summary.obligations())
        .extracting(o -> o.category(), o -> o.creditor(), o -> o.typicalMonthly(), o -> o.months())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                TransactionCategory.MORTGAGE_PAYMENT, "LHV Pank", new BigDecimal("712.40"), 6),
            org.assertj.core.groups.Tuple.tuple(
                TransactionCategory.LEASING, "Autoliising AS", new BigDecimal("289.00"), 6));
    assertThat(summary.monthlyObligations()).isEqualByComparingTo("1001.40");
    assertThat(summary.oneOffPayments())
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.creditor()).isEqualTo("Autoliising AS");
              assertThat(p.amount()).isEqualByComparingTo("3500.00");
            });
    assertThat(summary.monthlyCardRepayments()).isEqualByComparingTo("1200.00");
    // 1001.40 / 2480.00
    assertThat(summary.debtServiceRatio()).isEqualByComparingTo("0.40");
    assertThat(summary.monthlySavings()).isEqualByComparingTo("100.00");
    assertThat(summary.lowestBalance()).isEqualByComparingTo("312.55");
    assertThat(summary.holderCheck()).isEqualTo(HolderCheck.NOT_CHECKED);
    assertThat(summary.flags()).containsExactly(new Flag(LATE_FEES, 1));
  }

  @Test
  void flagsAMissingSalaryAShortPeriodAndANegativeBalance() {
    var statement =
        new ExtractedStatement(
            null,
            null,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 9, 30),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("-55.10"),
            List.of(
                tx(
                    "2026-08-20",
                    "Inkasso OÜ",
                    "Võlg",
                    "-120.00",
                    TransactionCategory.DEBT_COLLECTION),
                tx(
                    "2026-09-20",
                    "Inkasso OÜ",
                    "Võlg",
                    "-120.00",
                    TransactionCategory.DEBT_COLLECTION)));

    var summary = analyzer.analyze(statement, null, null);

    assertThat(summary.monthsCovered()).isEqualByComparingTo("2.0");
    assertThat(summary.debtServiceRatio()).isNull();
    assertThat(summary.flags())
        .containsExactly(
            new Flag(NO_SALARY, 1),
            new Flag(SHORT_PERIOD, 1),
            new Flag(NEGATIVE_BALANCE, 1),
            new Flag(DEBT_COLLECTION, 2));
  }

  @Test
  void flagsASalaryMonthFarBelowTheAverage() {
    var statement =
        new ExtractedStatement(
            null,
            null,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 9, 30),
            null,
            null,
            null,
            List.of(
                tx("2026-04-30", "Firma OÜ", "Palk", "2000.00", TransactionCategory.SALARY),
                tx("2026-05-31", "Firma OÜ", "Palk", "2000.00", TransactionCategory.SALARY),
                tx("2026-06-30", "Firma OÜ", "Palk", "2000.00", TransactionCategory.SALARY),
                tx("2026-07-31", "Firma OÜ", "Palk", "2000.00", TransactionCategory.SALARY),
                tx("2026-08-31", "Firma OÜ", "Palk", "2000.00", TransactionCategory.SALARY),
                tx("2026-09-30", "Firma OÜ", "Palk", "500.00", TransactionCategory.SALARY)));

    assertThat(analyzer.analyze(statement, null, null).flags())
        .containsExactly(new Flag(IRREGULAR_SALARY, 1));
  }

  @Test
  void neverKeepsThePayeeNameForChildSupport() {
    var statement =
        new ExtractedStatement(
            null,
            null,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 9, 30),
            null,
            null,
            null,
            List.of(
                tx(
                    "2026-05-05",
                    "Private Person",
                    "Elatis",
                    "-200.00",
                    TransactionCategory.CHILD_SUPPORT)));

    assertThat(analyzer.analyze(statement, null, null).obligations())
        .singleElement()
        .satisfies(o -> assertThat(o.creditor()).isNull());
  }

  @Test
  void flagsAStatementForSomeoneElse() {
    var statement =
        new ExtractedStatement("MARI MAASIKAS", null, null, null, null, null, null, List.of());

    var summary = analyzer.analyze(statement, "Jaan", "Tamm");

    assertThat(summary.holderCheck()).isEqualTo(HolderCheck.MISMATCH);
    assertThat(summary.flags()).contains(new Flag(HOLDER_MISMATCH, 1));
  }

  @Test
  void aCreditCardIsUsageNotADebtObligation() {
    var statement =
        statement(
            tx("2026-04-30", "Firma OÜ", "Palk", "3000.00", TransactionCategory.SALARY),
            tx("2026-05-31", "Firma OÜ", "Palk", "3000.00", TransactionCategory.SALARY),
            tx("2026-06-30", "Firma OÜ", "Palk", "3000.00", TransactionCategory.SALARY),
            tx(
                "2026-05-08",
                "Mari Maasikas",
                "Krediidi tagasimakse",
                "-2400.00",
                TransactionCategory.CREDIT_CARD),
            tx(
                "2026-06-08",
                "Mari Maasikas",
                "Krediidi tagasimakse",
                "-2100.00",
                TransactionCategory.CREDIT_CARD));

    var summary = analyzer.analyze(statement, null, null);

    assertThat(summary.obligations()).isEmpty();
    assertThat(summary.monthlyObligations()).isEqualByComparingTo("0");
    assertThat(summary.debtServiceRatio()).isEqualByComparingTo("0");
    // 4500 over the 6-month statement
    assertThat(summary.monthlyCardRepayments()).isEqualByComparingTo("750.00");
  }

  @Test
  void theTypicalInstalmentIgnoresAOneOffPayment() {
    var statement =
        statement(
            tx("2026-04-16", "Liising AS", "Sissemakse", "-4324.00", TransactionCategory.LEASING),
            tx("2026-05-08", "Liising AS", "Arve", "-314.41", TransactionCategory.LEASING),
            tx("2026-06-08", "Liising AS", "Arve", "-291.25", TransactionCategory.LEASING),
            tx("2026-07-09", "Liising AS", "Arve", "-293.11", TransactionCategory.LEASING),
            tx("2026-08-07", "Liising AS", "Arve", "-293.08", TransactionCategory.LEASING),
            tx("2026-09-07", "Liising AS", "Arve", "-293.05", TransactionCategory.LEASING),
            tx("2026-10-09", "Liising AS", "Arve", "-296.84", TransactionCategory.LEASING));

    var summary = analyzer.analyze(statement, null, null);

    assertThat(summary.obligations())
        .singleElement()
        .satisfies(
            o -> {
              // median of the six regular instalments
              assertThat(o.typicalMonthly()).isEqualByComparingTo("293.10");
              assertThat(o.months()).isEqualTo(6);
            });
    assertThat(summary.oneOffPayments())
        .singleElement()
        .satisfies(p -> assertThat(p.amount()).isEqualByComparingTo("4324.00"));
  }

  @Test
  void aRarePaymentIsSpreadOverTheStatement() {
    var statement =
        statement(
            tx(
                "2026-05-05",
                "Kindlustus AS",
                "Laen",
                "-600.00",
                TransactionCategory.CONSUMER_LOAN));

    assertThat(analyzer.analyze(statement, null, null).obligations())
        .singleElement()
        .satisfies(o -> assertThat(o.typicalMonthly()).isEqualByComparingTo("100.00"));
  }

  @Test
  void aLongOldStatementIsAnalysedOverItsLastSixFullMonthsAndFlaggedAsStale() {
    var transactions = new java.util.ArrayList<Transaction>();
    for (int month = 2; month <= 12; month++) {
      String amount = month <= 6 ? "1350.00" : month <= 10 ? "1540.00" : "1930.00";
      transactions.add(
          tx(
              LocalDate.of(2022, month, 1).toString(),
              "Firma OÜ",
              "Töötasu",
              amount,
              TransactionCategory.SALARY));
    }
    var statement =
        new ExtractedStatement(
            null,
            null,
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2023, 1, 1),
            null,
            null,
            null,
            transactions);

    var summary = analyzer.analyze(statement, null, null);

    assertThat(summary.monthsCovered()).isEqualByComparingTo("12.0");
    assertThat(summary.analysedFrom()).isEqualTo(LocalDate.of(2022, 7, 1));
    assertThat(summary.analysedTo()).isEqualTo(LocalDate.of(2022, 12, 31));
    assertThat(summary.monthsAnalysed()).isEqualByComparingTo("6.0");
    // Jul–Oct 1540, Nov–Dec 1930: the raise shows instead of being averaged away
    assertThat(summary.income().averageMonthlySalary()).isEqualByComparingTo("1670.00");
    assertThat(summary.income().monthsWithSalary()).isEqualTo(6);
    assertThat(summary.flags()).containsExactly(new Flag(STALE_STATEMENT, 1));
  }

  @Test
  void withoutRecencyChecksAnOldStatementIsAnalysedWhole() {
    var relaxed =
        new StatementAnalyzer(
            Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Tallinn")),
            new StatementProperties(false));
    var statement =
        new ExtractedStatement(
            null,
            null,
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2023, 1, 1),
            null,
            null,
            null,
            List.of(
                tx("2022-02-01", "Firma OÜ", "Töötasu", "1200.00", TransactionCategory.SALARY)));

    var summary = relaxed.analyze(statement, null, null);

    assertThat(summary.analysedFrom()).isEqualTo(LocalDate.of(2022, 1, 1));
    assertThat(summary.monthsAnalysed()).isEqualByComparingTo("12.0");
    assertThat(summary.flags()).extracting(Flag::code).doesNotContain(STALE_STATEMENT);
  }

  @Test
  void otherIncomeCountsOnlyWhenItRecurs() {
    var statement =
        statement(
            tx(
                "2026-05-07",
                "Eesti Töötukassa",
                "Hüvitis",
                "746.13",
                TransactionCategory.OTHER_INCOME),
            tx(
                "2026-06-03",
                "Linnakantselei",
                "Energiatoetus",
                "45.27",
                TransactionCategory.OTHER_INCOME),
            tx(
                "2026-05-20",
                "Sotsiaalkindlustusamet",
                "Peretoetus",
                "80.00",
                TransactionCategory.OTHER_INCOME),
            tx(
                "2026-06-20",
                "Sotsiaalkindlustusamet",
                "Peretoetus",
                "80.00",
                TransactionCategory.OTHER_INCOME),
            tx(
                "2026-07-20",
                "Sotsiaalkindlustusamet",
                "Peretoetus",
                "80.00",
                TransactionCategory.OTHER_INCOME));

    var income = analyzer.analyze(statement, null, null).income();

    // 3 × 80 over the 6-month statement
    assertThat(income.averageMonthlyOtherIncome()).isEqualByComparingTo("40.00");
    assertThat(income.oneOffIncome())
        .extracting(StatementSummary.OneOffIncome::payer)
        .containsExactly("Eesti Töötukassa", "Linnakantselei");
  }

  @ParameterizedTest
  @CsvSource({
    "MARI-LIIS ÕUNAPUU, Mari-Liis, Õunapuu, MATCH",
    "Mari-Liis Ounapuu, MARI-LIIS, ÕUNAPUU, MATCH",
    "'ÕUNAPUU, MARI-LIIS', Mari-Liis, Õunapuu, MATCH",
    "MARI ÕUNAPUU, Mari-Liis, Õunapuu, MISMATCH",
    "'', Mari, Õunapuu, NOT_CHECKED",
  })
  void matchesTheHolderToTheBorrower(
      String holder, String firstName, String lastName, HolderCheck expected) {
    assertThat(StatementAnalyzer.holderCheck(holder, firstName, lastName)).isEqualTo(expected);
  }

  private static ExtractedStatement statement(Transaction... transactions) {
    return new ExtractedStatement(
        null,
        null,
        LocalDate.of(2026, 4, 10),
        LocalDate.of(2026, 10, 10),
        null,
        null,
        null,
        List.of(transactions));
  }

  private static Transaction tx(
      String date, String counterparty, String description, String amount, TransactionCategory c) {
    return new Transaction(
        LocalDate.parse(date), counterparty, description, new BigDecimal(amount), c);
  }
}
