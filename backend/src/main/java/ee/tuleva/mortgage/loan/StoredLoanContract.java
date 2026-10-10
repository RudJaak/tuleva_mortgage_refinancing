package ee.tuleva.mortgage.loan;

import jakarta.persistence.Entity;
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
@Table(name = "loan_contract")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class StoredLoanContract {

  @Id private UUID id;
  private UUID documentId;
  private UUID borrowerId;
  private @Nullable String lender;
  private @Nullable BigDecimal margin;
  private @Nullable LocalDate maturityDate;
  private @Nullable BigDecimal principal;
  private @Nullable BigDecimal balance;
  private boolean balanceEstimated;

  @JdbcTypeCode(SqlTypes.JSON)
  private String contract;

  @JdbcTypeCode(SqlTypes.JSON)
  private String summary;

  private Instant createdAt;

  static StoredLoanContract of(
      UUID documentId,
      UUID borrowerId,
      ExtractedLoanContract contract,
      LoanSummary summary,
      String contractJson,
      String summaryJson,
      Instant createdAt) {
    return new StoredLoanContract(
        UUID.randomUUID(),
        documentId,
        borrowerId,
        contract.lender(),
        contract.margin(),
        summary.maturityDate(),
        contract.principal(),
        summary.balance(),
        summary.balanceSource() == LoanSummary.BalanceSource.ESTIMATED,
        contractJson,
        summaryJson,
        createdAt);
  }
}
