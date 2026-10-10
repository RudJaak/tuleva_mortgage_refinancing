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
      balanceSource = BalanceSource.STATED;
      balance = contract.balance();
    } else {
      balance = estimateBalance(contract, maturity, today);
      balanceSource = balance == null ? BalanceSource.UNKNOWN : BalanceSource.ESTIMATED;
    }
    BigDecimal loanToValue =
        balance == null
                || contract.propertyValue() == null
                || contract.propertyValue().signum() <= 0
            ? null
            : balance.divide(contract.propertyValue(), 2, HALF_UP);

    PersonNames.Match partyCheck = partyCheck(contract.borrowers(), firstName, lastName);
    // The logged-in borrower's age says nothing about someone else's contract
    Integer ageAtMaturity =
        birthDate == null || maturity == null || partyCheck == PersonNames.Match.MISMATCH
            ? null
            : Period.between(birthDate, maturity).getYears();
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
        loanToValue,
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
   * The original principal amortised to today. Contracts and information sheets state the
   * instalment as it was at signing, so the schedule is rebuilt at the rate that instalment
   * implies; without an instalment the margin is used, as Euribor was floored at zero for most
   * contracts signed before 2022. Equal principal repayments fall linearly. Approximate either way:
   * the instalment is recalculated at every Euribor reset.
   */
  static @Nullable BigDecimal estimateBalance(
      ExtractedLoanContract contract, @Nullable LocalDate maturity, LocalDate today) {
    BigDecimal principal = contract.principal();
    LocalDate start = contract.contractDate();
    Integer term = contract.termMonths();
    if (term == null && start != null && maturity != null) {
      term = (int) ChronoUnit.MONTHS.between(start, maturity);
    }
    if (principal == null || start == null || term == null || term <= 0) {
      return null;
    }
    int elapsed = (int) Math.clamp(ChronoUnit.MONTHS.between(start, today), 0, term);
    if (contract.repaymentType() == RepaymentType.EQUAL_PRINCIPAL) {
      return principal
          .multiply(BigDecimal.valueOf(term - elapsed))
          .divide(BigDecimal.valueOf(term), 0, HALF_UP);
    }
    Double monthlyRate;
    if (contract.monthlyPayment() != null) {
      monthlyRate =
          impliedMonthlyRate(
              principal.doubleValue(), term, contract.monthlyPayment().doubleValue());
    } else if (contract.statedInterestRate() != null) {
      monthlyRate = contract.statedInterestRate().doubleValue() / 100 / 12;
    } else if (contract.margin() != null) {
      monthlyRate = contract.margin().doubleValue() / 100 / 12;
    } else {
      return null;
    }
    double p = principal.doubleValue();
    double remaining =
        monthlyRate == 0
            ? p * (term - elapsed) / term
            : p
                * (Math.pow(1 + monthlyRate, term) - Math.pow(1 + monthlyRate, elapsed))
                / (Math.pow(1 + monthlyRate, term) - 1);
    return BigDecimal.valueOf(remaining).setScale(0, HALF_UP);
  }

  /** The monthly rate at which {@code payment} repays {@code principal} in {@code months}. */
  static double impliedMonthlyRate(double principal, int months, double payment) {
    if (payment * months <= principal) {
      return 0;
    }
    double low = 0;
    double high = 0.05;
    for (int i = 0; i < 100; i++) {
      double mid = (low + high) / 2;
      double annuity = principal * mid / (1 - Math.pow(1 + mid, -months));
      if (annuity > payment) {
        high = mid;
      } else {
        low = mid;
      }
    }
    return (low + high) / 2;
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
