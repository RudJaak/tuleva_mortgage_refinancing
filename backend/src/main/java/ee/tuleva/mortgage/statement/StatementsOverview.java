package ee.tuleva.mortgage.statement;

import ee.tuleva.mortgage.statement.StatementSummary.FlagCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Statements grouped per person and added up. Each borrower on the loan provides six months of
 * statements for every bank account they have; a person with none yet is listed with no documents.
 */
record StatementsOverview(List<Person> persons, Totals household) {

  /** {@code onLoan} is false for a holder who is not a borrower named on the contract. */
  record Person(
      String name, boolean onLoan, List<UUID> documentIds, Totals totals, Set<FlagCode> flags) {}

  record Totals(
      BigDecimal monthlyIncome,
      BigDecimal monthlyObligations,
      BigDecimal monthlyCardRepayments,
      BigDecimal monthlySavings,
      @Nullable BigDecimal debtServiceRatio) {}
}
