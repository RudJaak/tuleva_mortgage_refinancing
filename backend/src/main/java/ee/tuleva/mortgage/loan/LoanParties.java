package ee.tuleva.mortgage.loan;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Who is on the borrower's loan, as named in the contract they uploaded. */
@Component
@RequiredArgsConstructor
public class LoanParties {

  private final LoanRepository loans;
  private final StoredLoanContractRepository contracts;
  private final JsonMapper jsonMapper;

  /** The confirmed loan's contract, else the latest uploaded one; empty without a contract. */
  @Transactional(readOnly = true)
  public List<String> borrowerNames(UUID borrowerId) {
    var stored =
        loans
            .findByBorrowerId(borrowerId)
            .flatMap(loan -> java.util.Optional.ofNullable(loan.getLoanContractId()))
            .flatMap(contracts::findById)
            .or(() -> contracts.findFirstByBorrowerIdOrderByCreatedAtDesc(borrowerId));
    return stored
        .map(c -> jsonMapper.readValue(c.getContract(), ExtractedLoanContract.class).borrowers())
        .orElse(List.of());
  }
}
