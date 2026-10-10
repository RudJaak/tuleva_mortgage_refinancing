package ee.tuleva.mortgage.creditcheck;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credit_check")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class CreditCheck {

  enum Result {
    CLEAR,
    DEFAULTS_FOUND
  }

  @Id private UUID id;
  private UUID borrowerId;

  @Enumerated(EnumType.STRING)
  private Result result;

  private int activeDefaults;
  private String source;
  private Instant checkedAt;

  static CreditCheck of(UUID borrowerId, CreditCheckProvider.Report report, Instant checkedAt) {
    return new CreditCheck(
        UUID.randomUUID(),
        borrowerId,
        report.activeDefaults() == 0 ? Result.CLEAR : Result.DEFAULTS_FOUND,
        report.activeDefaults(),
        report.source(),
        checkedAt);
  }
}
