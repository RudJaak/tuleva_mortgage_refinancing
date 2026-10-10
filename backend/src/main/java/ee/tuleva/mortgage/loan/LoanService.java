package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import ee.tuleva.mortgage.borrower.PersonalCode;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Evidence;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The borrower's confirmed loan: what they accepted or corrected from the contract, with the source
 * of every field, and the screening figures recalculated on those values.
 */
@Service
@RequiredArgsConstructor
class LoanService {

  record LoanView(
      LoanTerms terms,
      Map<LoanField, LoanTerms.Source> provenance,
      Map<LoanField, Evidence> evidence,
      LoanSummary summary,
      Instant confirmedAt) {}

  private final LoanRepository loans;
  private final StoredLoanContractRepository contracts;
  private final LoanAnalyzer analyzer;
  private final BorrowerService borrowers;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  @Transactional
  LoanView confirm(UUID borrowerId, LoanTerms terms) {
    StoredLoanContract stored =
        terms.documentId() == null
            ? null
            : contracts
                .findByDocumentIdAndBorrowerId(terms.documentId(), borrowerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    ExtractedLoanContract read = stored == null ? null : read(stored);
    var provenance = terms.provenance(read);
    Loan loan = loans.findByBorrowerId(borrowerId).orElseGet(() -> Loan.of(borrowerId));
    loan.confirm(
        terms,
        stored == null ? null : stored.getId(),
        jsonMapper.writeValueAsString(provenance),
        clock.instant());
    loans.save(loan);
    return view(loan, terms, read, provenance);
  }

  @Transactional(readOnly = true)
  Optional<LoanView> current(UUID borrowerId) {
    return loans
        .findByBorrowerId(borrowerId)
        .map(
            loan -> {
              StoredLoanContract stored =
                  loan.getLoanContractId() == null
                      ? null
                      : contracts.findById(loan.getLoanContractId()).orElse(null);
              Map<LoanField, LoanTerms.Source> provenance =
                  jsonMapper.readValue(loan.getProvenance(), new TypeReference<>() {});
              return view(
                  loan,
                  loan.terms(stored == null ? null : stored.getDocumentId()),
                  stored == null ? null : read(stored),
                  provenance);
            });
  }

  /** Screening figures for a contract, with the logged-in borrower's name and age. */
  LoanSummary analyze(
      ExtractedLoanContract contract, UUID borrowerId, @Nullable Boolean signedByBorrower) {
    var borrower = borrowers.find(borrowerId);
    String personalCode = borrower.map(Borrower::getPersonalCode).orElse(null);
    LocalDate birthDate =
        personalCode != null && PersonalCode.isValid(personalCode)
            ? PersonalCode.birthDate(personalCode)
            : null;
    return analyzer.analyze(
        contract,
        borrower.map(Borrower::getFirstName).orElse(null),
        borrower.map(Borrower::getLastName).orElse(null),
        birthDate,
        signedByBorrower);
  }

  private LoanView view(
      Loan loan,
      LoanTerms terms,
      @Nullable ExtractedLoanContract read,
      Map<LoanField, LoanTerms.Source> provenance) {
    var contract = terms.toContract(read, LocalDate.now(clock));
    return new LoanView(
        terms,
        provenance,
        contract.evidence(),
        analyze(contract, loan.getBorrowerId(), null),
        loan.getConfirmedAt());
  }

  private ExtractedLoanContract read(StoredLoanContract stored) {
    return jsonMapper.readValue(stored.getContract(), ExtractedLoanContract.class);
  }
}
