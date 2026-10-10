package ee.tuleva.mortgage.loan;

import static java.math.RoundingMode.HALF_UP;

import ee.tuleva.mortgage.borrower.PersonNames;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Confidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import ee.tuleva.mortgage.loan.LoanSummary.BalanceSource;
import ee.tuleva.mortgage.loan.LoanSummary.Flag;
import ee.tuleva.mortgage.loan.LoanSummary.RateBasis;
import ee.tuleva.mortgage.pool.CalculatorProperties;
import ee.tuleva.mortgage.pool.PoolProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Derives screening figures from a contract. The extractor only reads; everything computed here is
 * deterministic and tested. Thresholds follow the risk-class gates in TASKS.md (T35).
 */
@Component
@RequiredArgsConstructor
class LoanAnalyzer {

  /** The pool targets balances of at least this much (T35 gate). */
  static final BigDecimal MINIMUM_BALANCE = new BigDecimal("50000");

  /** Refinancing needs at least ten years left to be worth a switch (T35 gate). */
  static final int MINIMUM_REMAINING_MONTHS = 120;

  private final Clock clock;
  private final CalculatorProperties calculator;
  private final PoolProperties pool;

  LoanSummary analyze(
      ExtractedLoanContract contract,
      @Nullable String firstName,
      @Nullable String lastName,
      @Nullable LocalDate birthDate,
      @Nullable Boolean signedByBorrower) {
    LocalDate today = LocalDate.now(clock);
    LocalDate maturity = maturity(contract);
    Integer remainingMonths =
        maturity == null ? null : (int) Math.max(0, ChronoUnit.MONTHS.between(today, maturity));

    boolean fixedNow =
        contract.rateType() == RateType.FIXED
            || (contract.rateType() == RateType.MIXED
                && contract.fixedRateUntil() != null
                && contract.fixedRateUntil().isAfter(today));
    RateBasis basis;
    BigDecimal rate;
    if (fixedNow) {
      basis = contract.statedInterestRate() != null ? RateBasis.FIXED : RateBasis.UNKNOWN;
      rate = contract.statedInterestRate();
    } else if (contract.margin() != null) {
      basis = RateBasis.EURIBOR_PLUS_MARGIN;
      rate = calculator.euribor6m().add(contract.margin());
    } else {
      basis = RateBasis.UNKNOWN;
      rate = null;
    }

    BalanceSource balanceSource;
    BigDecimal balance;
    if (contract.balance() != null) {
      balanceSource = BalanceSource.CONTRACT;
      balance = contract.balance();
    } else {
      balance = estimateBalance(contract, rate, remainingMonths);
      balanceSource = balance == null ? BalanceSource.UNKNOWN : BalanceSource.ESTIMATED;
    }

    Integer ageAtMaturity =
        birthDate == null || maturity == null
            ? null
            : Period.between(birthDate, maturity).getYears();
    PersonNames.Match partyCheck = partyCheck(contract.borrowers(), firstName, lastName);
    List<LoanField> missing = missingKeyFields(contract);
    List<LoanField> lowConfidence =
        contract.evidence().entrySet().stream()
            .filter(e -> e.getValue().confidence() == Confidence.LOW)
            .map(java.util.Map.Entry::getKey)
            .sorted()
            .toList();

    List<Flag> flags = new ArrayList<>();
    if (fixedNow) {
      flags.add(Flag.FIXED_RATE_PERIOD);
    }
    if (contract.margin() != null && contract.margin().compareTo(firstTargetMargin()) <= 0) {
      flags.add(Flag.MARGIN_AT_OR_BELOW_TARGET);
    }
    if (remainingMonths != null && remainingMonths < MINIMUM_REMAINING_MONTHS) {
      flags.add(Flag.REMAINING_TERM_UNDER_MINIMUM);
    }
    if (balance != null && balance.compareTo(MINIMUM_BALANCE) < 0) {
      flags.add(Flag.BALANCE_UNDER_MINIMUM);
    }
    if (contract.borrowers().size() > 1) {
      flags.add(Flag.CO_BORROWERS);
    }
    if (partyCheck == PersonNames.Match.MISMATCH) {
      flags.add(Flag.BORROWER_NOT_A_PARTY);
    }
    if (Boolean.FALSE.equals(signedByBorrower)) {
      flags.add(Flag.NOT_SIGNED_BY_BORROWER);
    }
    if (!missing.isEmpty()) {
      flags.add(Flag.MISSING_KEY_FIELDS);
    }

    return new LoanSummary(
        rate,
        basis,
        calculator.euribor6m(),
        calculator.euriborDate(),
        maturity,
        remainingMonths,
        balance,
        balanceSource,
        ageAtMaturity,
        contract.borrowers().size(),
        partyCheck,
        missing,
        lowConfidence,
        flags);
  }

  /** The stated maturity, or the contract date plus the term when only those are given. */
  private static @Nullable LocalDate maturity(ExtractedLoanContract contract) {
    if (contract.maturityDate() != null) {
      return contract.maturityDate();
    }
    if (contract.contractDate() != null && contract.termMonths() != null) {
      return contract.contractDate().plusMonths(contract.termMonths());
    }
    return null;
  }

  /**
   * For an annuity the outstanding principal is the present value of the remaining instalments at
   * the current rate; Euribor resets keep the instalment consistent with that. For equal principal
   * repayments it falls linearly over the term. Approximate either way.
   */
  static @Nullable BigDecimal estimateBalance(
      ExtractedLoanContract contract, @Nullable BigDecimal annualRate, @Nullable Integer months) {
    if (months == null) {
      return null;
    }
    if (contract.repaymentType() == RepaymentType.EQUAL_PRINCIPAL) {
      if (contract.principal() == null
          || contract.termMonths() == null
          || contract.termMonths() == 0) {
        return null;
      }
      return contract
          .principal()
          .multiply(BigDecimal.valueOf(Math.min(months, contract.termMonths())))
          .divide(BigDecimal.valueOf(contract.termMonths()), 0, HALF_UP);
    }
    if (contract.monthlyPayment() == null || annualRate == null) {
      return null;
    }
    double r = annualRate.doubleValue() / 100 / 12;
    double payment = contract.monthlyPayment().doubleValue();
    double value = r == 0 ? payment * months : payment * (1 - Math.pow(1 + r, -months)) / r;
    return BigDecimal.valueOf(value).setScale(0, HALF_UP);
  }

  private BigDecimal firstTargetMargin() {
    return pool.milestones().stream()
        .min(Comparator.comparing(PoolProperties.Milestone::volume))
        .map(PoolProperties.Milestone::margin)
        .orElse(BigDecimal.ZERO);
  }

  private static PersonNames.Match partyCheck(
      List<String> borrowers, @Nullable String firstName, @Nullable String lastName) {
    if (borrowers.isEmpty()) {
      return PersonNames.Match.NOT_CHECKED;
    }
    return borrowers.stream()
            .anyMatch(b -> PersonNames.match(b, firstName, lastName) == PersonNames.Match.MATCH)
        ? PersonNames.Match.MATCH
        : borrowers.stream()
                .allMatch(
                    b -> PersonNames.match(b, firstName, lastName) == PersonNames.Match.NOT_CHECKED)
            ? PersonNames.Match.NOT_CHECKED
            : PersonNames.Match.MISMATCH;
  }

  /** Without the lender, margin and maturity the borrower's saving cannot be estimated. */
  private static List<LoanField> missingKeyFields(ExtractedLoanContract contract) {
    List<LoanField> missing = new ArrayList<>();
    if (contract.lender() == null) {
      missing.add(LoanField.LENDER);
    }
    if (contract.margin() == null && contract.rateType() != RateType.FIXED) {
      missing.add(LoanField.MARGIN);
    }
    if (contract.maturityDate() == null
        && (contract.contractDate() == null || contract.termMonths() == null)) {
      missing.add(LoanField.MATURITY_DATE);
    }
    return missing;
  }
}
