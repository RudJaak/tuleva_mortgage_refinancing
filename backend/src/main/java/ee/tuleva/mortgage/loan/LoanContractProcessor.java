package ee.tuleva.mortgage.loan;

import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.EXTRACTION_FAILED;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.NO_USABLE_FILE;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import ee.tuleva.mortgage.borrower.PersonalCode;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.document.DocumentProcessor;
import ee.tuleva.mortgage.document.DocumentRejectedException;
import ee.tuleva.mortgage.document.DocumentType;
import ee.tuleva.mortgage.document.ReceivedDocument;
import ee.tuleva.mortgage.extraction.ExtractionException;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loan contracts: read the fields with their evidence, derive the screening figures and keep both.
 * The borrower confirms or corrects the fields in the loan step (T30).
 */
@Component
@RequiredArgsConstructor
class LoanContractProcessor implements DocumentProcessor {

  record LoanContractResult(ExtractedLoanContract contract, LoanSummary summary) {}

  private final LoanContractExtractor extractor;
  private final LoanAnalyzer analyzer;
  private final StoredLoanContractRepository contracts;
  private final BorrowerService borrowers;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  @Override
  public DocumentType type() {
    return DocumentType.LOAN_CONTRACT;
  }

  @Override
  public LoanContractResult process(ReceivedDocument document) {
    DocumentFile pdf =
        document.files().stream()
            .filter(DocumentFile::isPdf)
            .findFirst()
            .orElseThrow(
                () -> new DocumentRejectedException(NO_USABLE_FILE, "No PDF contract found"));
    ExtractedLoanContract contract;
    try {
      contract = extractor.extract(pdf);
    } catch (ExtractionException e) {
      throw new DocumentRejectedException(EXTRACTION_FAILED, e.getMessage(), e);
    }

    var borrower = borrowers.find(document.borrowerId());
    var personalCode = borrower.map(Borrower::getPersonalCode).orElse(null);
    LoanSummary summary =
        analyzer.analyze(
            contract,
            borrower.map(Borrower::getFirstName).orElse(null),
            borrower.map(Borrower::getLastName).orElse(null),
            personalCode != null && PersonalCode.isValid(personalCode)
                ? PersonalCode.birthDate(personalCode)
                : null,
            document.verification().signedByBorrower());
    contracts.save(
        StoredLoanContract.of(
            document.id(),
            document.borrowerId(),
            contract,
            summary,
            jsonMapper.writeValueAsString(contract),
            jsonMapper.writeValueAsString(summary),
            clock.instant()));
    return new LoanContractResult(contract, summary);
  }
}
