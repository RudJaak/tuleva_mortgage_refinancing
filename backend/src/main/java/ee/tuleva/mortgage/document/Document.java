package ee.tuleva.mortgage.document;

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
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Document {

  @Id private UUID id;
  private UUID borrowerId;

  @Enumerated(EnumType.STRING)
  private DocumentType type;

  private String sha256;

  @Enumerated(EnumType.STRING)
  private VerificationLevel verificationLevel;

  private @Nullable String sealedByBank;
  private @Nullable Instant sealedAt;
  private @Nullable Boolean signedByBorrower;

  @Enumerated(EnumType.STRING)
  private DocumentStatus status;

  private Instant createdAt;

  static Document received(
      UUID borrowerId,
      DocumentType type,
      String sha256,
      Verification verification,
      Instant createdAt) {
    return new Document(
        UUID.randomUUID(),
        borrowerId,
        type,
        sha256,
        verification.level(),
        verification.bankName(),
        verification.sealedAt(),
        verification.signedByBorrower(),
        DocumentStatus.RECEIVED,
        createdAt);
  }

  void processed() {
    status = DocumentStatus.PROCESSED;
  }

  void failed() {
    status = DocumentStatus.FAILED;
  }
}
