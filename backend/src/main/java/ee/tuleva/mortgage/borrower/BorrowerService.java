package ee.tuleva.mortgage.borrower;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BorrowerService {

  private final BorrowerRepository borrowers;
  private final Clock clock;

  @Transactional
  /** A login without a known name keeps the name a returning borrower already has. */
  public Borrower findOrCreate(
      String personalCode, @Nullable String firstName, @Nullable String lastName) {
    return borrowers
        .findByPersonalCode(personalCode)
        .map(
            existing -> {
              if (firstName != null) {
                existing.rename(firstName, lastName);
              }
              return existing;
            })
        .orElseGet(
            () ->
                borrowers.save(
                    Borrower.identified(personalCode, firstName, lastName, clock.instant())));
  }

  @Transactional(readOnly = true)
  public Optional<Borrower> find(UUID id) {
    return borrowers.findById(id);
  }
}
