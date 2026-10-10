package ee.tuleva.mortgage.statement;

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
@Table(name = "statement_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class StoredStatementSummary {

  @Id private UUID id;
  private UUID documentId;
  private UUID borrowerId;
  private @Nullable LocalDate periodStart;
  private @Nullable LocalDate periodEnd;
  private BigDecimal averageMonthlyIncome;
  private BigDecimal monthlyObligations;
  private @Nullable String accountHolder;
  private @Nullable String account;

  @JdbcTypeCode(SqlTypes.JSON)
  private String summary;

  private Instant createdAt;

  static StoredStatementSummary of(
      UUID documentId,
      UUID borrowerId,
      StatementSummary summary,
      @Nullable String accountHolder,
      @Nullable String account,
      String summaryJson,
      Instant createdAt) {
    return new StoredStatementSummary(
        UUID.randomUUID(),
        documentId,
        borrowerId,
        summary.periodStart(),
        summary.periodEnd(),
        summary.income().averageMonthlyTotal(),
        summary.monthlyObligations(),
        accountHolder,
        account,
        summaryJson,
        createdAt);
  }
}
