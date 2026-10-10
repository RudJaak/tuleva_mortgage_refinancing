package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The loan fields the borrower confirms or corrects. {@code documentId} links them to the contract
 * they were read from; without it every value is the borrower's own declaration.
 */
record LoanTerms(
    @Nullable UUID documentId,
    @Nullable String lender,
    @Nullable String contractNumber,
    @Nullable LocalDate contractDate,
    @Nullable BigDecimal principal,
    @Nullable BigDecimal margin,
    @Nullable String baseRate,
    RateType rateType,
    @Nullable LocalDate fixedRateUntil,
    @Nullable LocalDate maturityDate,
    @Nullable BigDecimal monthlyPayment,
    RepaymentType repaymentType,
    @Nullable BigDecimal balance,
    @Nullable BigDecimal propertyValue) {

  enum Source {
    /** Confirmed as read from the uploaded contract. */
    DOCUMENT,
    /** Entered or corrected by the borrower. */
    DECLARED
  }

  /** Where each filled field came from, compared with what was read from the contract. */
  Map<LoanField, Source> provenance(@Nullable ExtractedLoanContract read) {
    var provenance = new EnumMap<LoanField, Source>(LoanField.class);
    put(provenance, LoanField.LENDER, lender, read == null ? null : read.lender());
    put(
        provenance,
        LoanField.CONTRACT_NUMBER,
        contractNumber,
        read == null ? null : read.contractNumber());
    put(
        provenance,
        LoanField.CONTRACT_DATE,
        contractDate,
        read == null ? null : read.contractDate());
    put(provenance, LoanField.PRINCIPAL, principal, read == null ? null : read.principal());
    put(provenance, LoanField.MARGIN, margin, read == null ? null : read.margin());
    put(provenance, LoanField.BASE_RATE, baseRate, read == null ? null : read.baseRate());
    put(provenance, LoanField.RATE_TYPE, rateType, read == null ? null : read.rateType());
    put(
        provenance,
        LoanField.FIXED_RATE_UNTIL,
        fixedRateUntil,
        read == null ? null : read.fixedRateUntil());
    put(
        provenance,
        LoanField.MATURITY_DATE,
        maturityDate,
        read == null ? null : read.maturityDate());
    put(
        provenance,
        LoanField.MONTHLY_PAYMENT,
        monthlyPayment,
        read == null ? null : read.monthlyPayment());
    put(
        provenance,
        LoanField.REPAYMENT_TYPE,
        repaymentType,
        read == null ? null : read.repaymentType());
    put(provenance, LoanField.BALANCE, balance, read == null ? null : read.balance());
    put(
        provenance,
        LoanField.PROPERTY_VALUE,
        propertyValue,
        read == null ? null : read.propertyValue());
    return provenance;
  }

  /**
   * The confirmed terms as a contract for the analyzer: what the borrower confirmed, plus what was
   * only read (borrowers, term, collateral, evidence). A balance the borrower states is dated
   * today.
   */
  ExtractedLoanContract toContract(@Nullable ExtractedLoanContract read, LocalDate today) {
    boolean sameMaturity = read != null && Objects.equals(read.maturityDate(), maturityDate);
    return new ExtractedLoanContract(
        lender,
        contractNumber,
        contractDate,
        read == null ? java.util.List.of() : read.borrowers(),
        principal,
        read == null ? "EUR" : read.currency(),
        baseRate,
        margin,
        rateType,
        fixedRateUntil,
        read == null ? null : read.statedInterestRate(),
        maturityDate,
        sameMaturity ? read.termMonths() : null,
        monthlyPayment,
        read == null ? null : read.paymentDay(),
        repaymentType,
        read == null ? null : read.mortgageAmount(),
        read == null ? null : read.collateral(),
        read == null ? null : read.earlyRepaymentTerms(),
        propertyValue,
        balance,
        balance == null ? null : today,
        read == null ? Map.of() : read.evidence());
  }

  private static void put(
      Map<LoanField, Source> provenance,
      LoanField field,
      @Nullable Object confirmed,
      @Nullable Object read) {
    if (confirmed == null) {
      return;
    }
    provenance.put(field, same(confirmed, read) ? Source.DOCUMENT : Source.DECLARED);
  }

  private static boolean same(Object confirmed, @Nullable Object read) {
    if (confirmed instanceof BigDecimal a && read instanceof BigDecimal b) {
      return a.compareTo(b) == 0;
    }
    return confirmed.equals(read);
  }
}
