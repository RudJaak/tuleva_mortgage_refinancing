package ee.tuleva.mortgage.borrower;

import jakarta.persistence.Entity;
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
@Table(name = "borrower")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Borrower {

  @Id private UUID id;
  private @Nullable String personalCode;
  private @Nullable String firstName;
  private @Nullable String lastName;
  private boolean synthetic;
  private Instant createdAt;

  public static Borrower identified(
      String personalCode, String firstName, String lastName, Instant createdAt) {
    return new Borrower(UUID.randomUUID(), personalCode, firstName, lastName, false, createdAt);
  }

  public static Borrower synthetic(Instant createdAt) {
    return new Borrower(UUID.randomUUID(), null, null, null, true, createdAt);
  }
}
