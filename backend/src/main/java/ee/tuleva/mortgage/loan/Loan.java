package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "loan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Loan {

  @Id private UUID id;
  private UUID borrowerId;
  private @Nullable UUID loanContractId;
  private @Nullable String lender;
  private @Nullable String contractNumber;
  private @Nullable LocalDate contractDate;
  private @Nullable BigDecimal principal;
  private @Nullable BigDecimal margin;
  private @Nullable String baseRate;

  @Enumerated(EnumType.STRING)
  private RateType rateType;

  private @Nullable LocalDate fixedRateUntil;
  private @Nullable LocalDate maturityDate;
  private @Nullable BigDecimal monthlyPayment;

  @Enumerated(EnumType.STRING)
  private RepaymentType repaymentType;

  private @Nullable BigDecimal balance;
  private @Nullable BigDecimal propertyValue;

  @JdbcTypeCode(SqlTypes.JSON)
  private String provenance;

  private Instant confirmedAt;

  static Loan of(UUID borrowerId) {
    return new Loan(
        UUID.randomUUID(),
        borrowerId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        RateType.UNKNOWN,
        null,
        null,
        null,
        RepaymentType.UNKNOWN,
        null,
        null,
        "{}",
        Instant.EPOCH);
  }

  void confirm(
      LoanTerms terms, @Nullable UUID loanContractId, String provenanceJson, Instant confirmedAt) {
    this.loanContractId = loanContractId;
    this.lender = terms.lender();
    this.contractNumber = terms.contractNumber();
    this.contractDate = terms.contractDate();
    this.principal = terms.principal();
    this.margin = terms.margin();
    this.baseRate = terms.baseRate();
    this.rateType = terms.rateType();
    this.fixedRateUntil = terms.fixedRateUntil();
    this.maturityDate = terms.maturityDate();
    this.monthlyPayment = terms.monthlyPayment();
    this.repaymentType = terms.repaymentType();
    this.balance = terms.balance();
    this.propertyValue = terms.propertyValue();
    this.provenance = provenanceJson;
    this.confirmedAt = confirmedAt;
  }

  LoanTerms terms(@Nullable UUID documentId) {
    return new LoanTerms(
        documentId,
        lender,
        contractNumber,
        contractDate,
        principal,
        margin,
        baseRate,
        rateType,
        fixedRateUntil,
        maturityDate,
        monthlyPayment,
        repaymentType,
        balance,
        propertyValue);
  }
}
