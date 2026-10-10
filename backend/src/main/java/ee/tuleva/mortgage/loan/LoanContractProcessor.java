package ee.tuleva.mortgage.loan;

import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.EXTRACTION_FAILED;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.NO_USABLE_FILE;

import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.document.DocumentProcessor;
import ee.tuleva.mortgage.document.DocumentRejectedException;
import ee.tuleva.mortgage.document.DocumentType;
import ee.tuleva.mortgage.document.ReceivedDocument;
import ee.tuleva.mortgage.extraction.ExtractionException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
  private final LoanService loanService;
  private final StoredLoanContractRepository contracts;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  @Override
  public DocumentType type() {
    return DocumentType.LOAN_CONTRACT;
  }

  @Override
  public Optional<Object> storedResult(UUID documentId) {
    return contracts
        .findByDocumentId(documentId)
        .map(
            stored ->
                new LoanContractResult(
                    jsonMapper.readValue(stored.getContract(), ExtractedLoanContract.class),
                    jsonMapper.readValue(stored.getSummary(), LoanSummary.class)));
  }

  @Override
  public LoanContractResult process(ReceivedDocument document) {
    List<DocumentFile> pdfs = document.files().stream().filter(DocumentFile::isPdf).toList();
    if (pdfs.isEmpty()) {
      throw new DocumentRejectedException(NO_USABLE_FILE, "No PDF contract found");
    }
    ExtractedLoanContract contract;
    try {
      contract = extractor.extract(pdfs);
    } catch (ExtractionException e) {
      throw new DocumentRejectedException(EXTRACTION_FAILED, e.getMessage(), e);
    }

    LoanSummary summary =
        loanService.analyze(
            contract, document.borrowerId(), document.verification().signedByBorrower());
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
