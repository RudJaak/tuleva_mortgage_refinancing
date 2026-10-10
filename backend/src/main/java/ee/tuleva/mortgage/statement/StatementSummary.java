package ee.tuleva.mortgage.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Screening figures derived from one statement. This, not the transactions, is what we keep. */
record StatementSummary(
    @Nullable LocalDate periodStart,
    @Nullable LocalDate periodEnd,
    BigDecimal monthsCovered,
    @Nullable LocalDate analysedFrom,
    @Nullable LocalDate analysedTo,
    BigDecimal monthsAnalysed,
    Income income,
    List<Obligation> obligations,
    BigDecimal monthlyObligations,
    List<OneOffPayment> oneOffPayments,
    BigDecimal monthlyCardRepayments,
    @Nullable BigDecimal debtServiceRatio,
    BigDecimal monthlySavings,
    @Nullable BigDecimal lowestBalance,
    HolderCheck holderCheck,
    List<Flag> flags) {

  /** Summaries stored before a field existed come back without it; read those as empty. */
  StatementSummary {
    monthsAnalysed = monthsAnalysed == null ? monthsCovered : monthsAnalysed;
    obligations = obligations == null ? List.of() : obligations;
    oneOffPayments = oneOffPayments == null ? List.of() : oneOffPayments;
    monthlyCardRepayments = monthlyCardRepayments == null ? BigDecimal.ZERO : monthlyCardRepayments;
    flags = flags == null ? List.of() : flags;
  }

  record Income(
      BigDecimal averageMonthlySalary,
      BigDecimal lowestMonthlySalary,
      int monthsWithSalary,
      int employers,
      BigDecimal averageMonthlyOtherIncome,
      BigDecimal averageMonthlyTotal,
      List<OneOffIncome> oneOffIncome) {

    Income {
      oneOffIncome = oneOffIncome == null ? List.of() : oneOffIncome;
    }
  }

  /** Other income that does not recur, e.g. a single benefit payment; not in the averages. */
  record OneOffIncome(String payer, LocalDate date, BigDecimal amount) {}

  /**
   * A recurring payment at its typical (median) monthly amount; {@code creditor} is omitted when
   * the payee is a private person.
   */
  record Obligation(
      TransactionCategory category,
      @Nullable String creditor,
      BigDecimal typicalMonthly,
      int months) {}

  /** A payment far above the creditor's usual instalment, e.g. a down payment or a settlement. */
  record OneOffPayment(
      TransactionCategory category, @Nullable String creditor, LocalDate date, BigDecimal amount) {}

  record Flag(FlagCode code, int occurrences) {}

  enum FlagCode {
    NO_SALARY,
    IRREGULAR_SALARY,
    SHORT_PERIOD,
    STALE_STATEMENT,
    NEGATIVE_BALANCE,
    LATE_FEES,
    DEBT_COLLECTION,
    ENFORCEMENT,
    GAMBLING,
    HOLDER_MISMATCH
  }

  enum HolderCheck {
    MATCH,
    MISMATCH,
    NOT_CHECKED
  }
}
