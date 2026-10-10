package ee.tuleva.mortgage.pool;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "commitment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Commitment {

  @Id private UUID id;
  private UUID borrowerId;
  private BigDecimal committedBalance;
  private BigDecimal currentMargin;

  @Enumerated(EnumType.STRING)
  private CommitmentStatus status;

  private Instant signedAt;
  private @Nullable Instant withdrawnAt;

  public static Commitment signed(
      UUID borrowerId, BigDecimal committedBalance, BigDecimal currentMargin, Instant signedAt) {
    return new Commitment(
        UUID.randomUUID(),
        borrowerId,
        committedBalance,
        currentMargin,
        CommitmentStatus.SIGNED,
        signedAt,
        null);
  }

  public void withdraw(Instant at) {
    status = CommitmentStatus.WITHDRAWN;
    withdrawnAt = at;
  }
}
