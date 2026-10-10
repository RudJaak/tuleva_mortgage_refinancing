package ee.tuleva.mortgage.borrower;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "borrower")
public class Borrower {

  @Id private UUID id;

  @Column(name = "personal_code")
  private @Nullable String personalCode;

  @Column(name = "first_name")
  private @Nullable String firstName;

  @Column(name = "last_name")
  private @Nullable String lastName;

  private boolean synthetic;

  @Column(name = "created_at")
  private Instant createdAt;

  protected Borrower() {}

  private Borrower(
      @Nullable String personalCode,
      @Nullable String firstName,
      @Nullable String lastName,
      boolean synthetic,
      Instant createdAt) {
    this.id = UUID.randomUUID();
    this.personalCode = personalCode;
    this.firstName = firstName;
    this.lastName = lastName;
    this.synthetic = synthetic;
    this.createdAt = createdAt;
  }

  public static Borrower identified(
      String personalCode, String firstName, String lastName, Instant createdAt) {
    return new Borrower(personalCode, firstName, lastName, false, createdAt);
  }

  public static Borrower synthetic(Instant createdAt) {
    return new Borrower(null, null, null, true, createdAt);
  }

  public UUID getId() {
    return id;
  }

  public @Nullable String getPersonalCode() {
    return personalCode;
  }

  public boolean isSynthetic() {
    return synthetic;
  }
}
