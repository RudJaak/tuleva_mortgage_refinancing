package ee.tuleva.mortgage.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What an extractor reads from one statement: its frame, plus only the transactions that matter for
 * screening, already categorised. Amounts are signed: credits positive, debits negative.
 */
record ExtractedStatement(
    @Nullable String accountHolder,
    @Nullable String iban,
    @Nullable LocalDate periodStart,
    @Nullable LocalDate periodEnd,
    @Nullable BigDecimal openingBalance,
    @Nullable BigDecimal closingBalance,
    @Nullable BigDecimal lowestBalance,
    List<Transaction> transactions) {

  record Transaction(
      LocalDate date,
      String counterparty,
      String description,
      BigDecimal amount,
      TransactionCategory category) {}
}
