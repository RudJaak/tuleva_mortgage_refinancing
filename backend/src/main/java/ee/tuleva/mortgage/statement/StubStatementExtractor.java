package ee.tuleva.mortgage.statement;

import static ee.tuleva.mortgage.statement.TransactionCategory.CREDIT_CARD;
import static ee.tuleva.mortgage.statement.TransactionCategory.LATE_FEE;
import static ee.tuleva.mortgage.statement.TransactionCategory.LEASING;
import static ee.tuleva.mortgage.statement.TransactionCategory.MORTGAGE_PAYMENT;
import static ee.tuleva.mortgage.statement.TransactionCategory.OTHER_INCOME;
import static ee.tuleva.mortgage.statement.TransactionCategory.SALARY;
import static ee.tuleva.mortgage.statement.TransactionCategory.SAVINGS;

import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.statement.ExtractedStatement.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline stand-in for the LLM: returns the same synthetic six-month statement for any file. The
 * holder is left unknown, so the name check reports NOT_CHECKED instead of a false mismatch.
 */
class StubStatementExtractor implements StatementExtractor {

  static final LocalDate PERIOD_START = LocalDate.of(2026, 4, 10);
  static final LocalDate PERIOD_END = LocalDate.of(2026, 10, 10);

  @Override
  public ExtractedStatement extract(DocumentFile statement) {
    List<Transaction> transactions = new ArrayList<>();
    for (int month = 0; month < 6; month++) {
      LocalDate payday = LocalDate.of(2026, 4, 30).plusMonths(month);
      transactions.add(
          tx(payday, "Näidis Tarkvara OÜ", "Töötasu", month == 2 ? "2350.00" : "2410.00", SALARY));
      transactions.add(
          tx(payday.minusDays(10), "Sotsiaalkindlustusamet", "Peretoetus", "80.00", OTHER_INCOME));
      transactions.add(
          tx(payday.plusDays(5), "LHV Pank", "Kodulaenu tagasimakse", "-712.40", MORTGAGE_PAYMENT));
      transactions.add(
          tx(payday.plusDays(7), "Autoliising AS", "Liisingumakse", "-289.00", LEASING));
      transactions.add(
          tx(payday.plusDays(1), "Tuleva III sammas", "Sissemakse", "-100.00", SAVINGS));
      transactions.add(
          tx(
              payday.plusDays(2),
              "LHV krediitkaart",
              "Krediidi tagasimakse",
              "-1200.00",
              CREDIT_CARD));
    }
    transactions.add(
        tx(LocalDate.of(2026, 4, 15), "Autoliising AS", "Sissemakse", "-3500.00", LEASING));
    transactions.add(tx(LocalDate.of(2026, 8, 12), "Autoliising AS", "Viivis", "-4.20", LATE_FEE));
    return new ExtractedStatement(
        null,
        null,
        PERIOD_START,
        PERIOD_END,
        new BigDecimal("1840.12"),
        new BigDecimal("2215.37"),
        new BigDecimal("312.55"),
        transactions);
  }

  private static Transaction tx(
      LocalDate date,
      String counterparty,
      String description,
      String amount,
      TransactionCategory category) {
    return new Transaction(date, counterparty, description, new BigDecimal(amount), category);
  }
}
