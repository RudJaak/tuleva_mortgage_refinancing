package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.borrower.PersonNames;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Figures derived from a contract for screening; estimates are marked as such. */
record LoanSummary(
    @Nullable BigDecimal currentInterestRate,
    RateBasis rateBasis,
    BigDecimal euribor6m,
    LocalDate euriborDate,
    @Nullable LocalDate maturityDate,
    @Nullable Integer remainingMonths,
    @Nullable BigDecimal balance,
    BalanceSource balanceSource,
    @Nullable Integer ageAtMaturity,
    int borrowerCount,
    PersonNames.Match partyCheck,
    List<LoanField> missingFields,
    List<LoanField> lowConfidenceFields,
    List<Flag> flags) {

  enum RateBasis {
    /** 6M Euribor today plus the contract margin. */
    EURIBOR_PLUS_MARGIN,
    /** A fixed rate that is still running. */
    FIXED,
    UNKNOWN
  }

  enum BalanceSource {
    /** Stated in the uploaded document. */
    CONTRACT,
    /** Present value of the remaining instalments at today's rate: approximate. */
    ESTIMATED,
    UNKNOWN
  }

  enum Flag {
    FIXED_RATE_PERIOD,
    MARGIN_AT_OR_BELOW_TARGET,
    REMAINING_TERM_UNDER_MINIMUM,
    BALANCE_UNDER_MINIMUM,
    CO_BORROWERS,
    BORROWER_NOT_A_PARTY,
    NOT_SIGNED_BY_BORROWER,
    MISSING_KEY_FIELDS
  }
}
