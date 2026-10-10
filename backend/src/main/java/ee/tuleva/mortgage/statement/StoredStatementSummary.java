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

  @JdbcTypeCode(SqlTypes.JSON)
  private String summary;

  private Instant createdAt;

  static StoredStatementSummary of(
      UUID documentId,
      UUID borrowerId,
      StatementSummary summary,
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
        summaryJson,
        createdAt);
  }
}
