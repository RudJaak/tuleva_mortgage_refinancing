package ee.tuleva.mortgage.loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What an extractor reads from a loan contract upload, as stated in it; nothing derived. An upload
 * can hold the contract with its standard information sheet (ESIS) or repayment schedule; the
 * contract wins on conflicts and amendments override the original terms. {@code evidence} says
 * where each field was read, so the borrower can check it.
 */
record ExtractedLoanContract(
    @Nullable String lender,
    @Nullable String contractNumber,
    @Nullable LocalDate contractDate,
    List<String> borrowers,
    @Nullable BigDecimal principal,
    @Nullable String currency,
    @Nullable String baseRate,
    @Nullable BigDecimal margin,
    RateType rateType,
    @Nullable LocalDate fixedRateUntil,
    @Nullable BigDecimal statedInterestRate,
    @Nullable LocalDate maturityDate,
    @Nullable Integer termMonths,
    @Nullable BigDecimal monthlyPayment,
    @Nullable Integer paymentDay,
    RepaymentType repaymentType,
    @Nullable BigDecimal mortgageAmount,
    @Nullable String collateral,
    @Nullable String earlyRepaymentTerms,
    @Nullable BigDecimal propertyValue,
    @Nullable BigDecimal balance,
    @Nullable LocalDate balanceDate,
    Map<LoanField, Evidence> evidence) {

  enum RateType {
    VARIABLE,
    FIXED,
    MIXED,
    UNKNOWN
  }

  enum RepaymentType {
    ANNUITY,
    EQUAL_PRINCIPAL,
    UNKNOWN
  }

  enum Confidence {
    HIGH,
    MEDIUM,
    LOW
  }

  /**
   * {@code document} is the file within the upload (the contract, the standard information sheet, a
   * repayment schedule); {@code page} is 1-based; {@code quote} is the document's own wording.
   */
  record Evidence(String document, int page, String quote, Confidence confidence) {}
}
