package ee.tuleva.mortgage.pool;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "commitment")
public class Commitment {

  @Id private UUID id;

  @Column(name = "borrower_id")
  private UUID borrowerId;

  @Column(name = "committed_balance")
  private BigDecimal committedBalance;

  @Column(name = "current_margin")
  private BigDecimal currentMargin;

  @Enumerated(EnumType.STRING)
  private CommitmentStatus status;

  @Column(name = "signed_at")
  private Instant signedAt;

  @Column(name = "withdrawn_at")
  private @Nullable Instant withdrawnAt;

  protected Commitment() {}

  private Commitment(
      UUID borrowerId, BigDecimal committedBalance, BigDecimal currentMargin, Instant signedAt) {
    this.id = UUID.randomUUID();
    this.borrowerId = borrowerId;
    this.committedBalance = committedBalance;
    this.currentMargin = currentMargin;
    this.status = CommitmentStatus.SIGNED;
    this.signedAt = signedAt;
  }

  public static Commitment signed(
      UUID borrowerId, BigDecimal committedBalance, BigDecimal currentMargin, Instant signedAt) {
    return new Commitment(borrowerId, committedBalance, currentMargin, signedAt);
  }

  public void withdraw(Instant at) {
    status = CommitmentStatus.WITHDRAWN;
    withdrawnAt = at;
  }

  public UUID getId() {
    return id;
  }

  public UUID getBorrowerId() {
    return borrowerId;
  }

  public BigDecimal getCommittedBalance() {
    return committedBalance;
  }

  public CommitmentStatus getStatus() {
    return status;
  }
}
