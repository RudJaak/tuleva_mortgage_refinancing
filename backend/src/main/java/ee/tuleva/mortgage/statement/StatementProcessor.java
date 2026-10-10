package ee.tuleva.mortgage.statement;

import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.EXTRACTION_FAILED;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.NO_USABLE_FILE;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.document.DocumentProcessor;
import ee.tuleva.mortgage.document.DocumentRejectedException;
import ee.tuleva.mortgage.document.DocumentType;
import ee.tuleva.mortgage.document.ReceivedDocument;
import ee.tuleva.mortgage.extraction.ExtractionException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Account statements: extract the relevant transactions, derive the screening summary and keep only
 * the summary. The transactions go back to the borrower once, so they can see what was counted.
 */
@Component
@RequiredArgsConstructor
class StatementProcessor implements DocumentProcessor {

  record StatementResult(
      @Nullable String account, StatementSummary summary, List<CountedTransaction> counted) {}

  record CountedTransaction(
      LocalDate date,
      @Nullable String counterparty,
      String description,
      BigDecimal amount,
      TransactionCategory category) {}

  private final StatementExtractor extractor;
  private final StatementAnalyzer analyzer;
  private final StoredStatementSummaryRepository summaries;
  private final BorrowerService borrowers;
  private final JsonMapper jsonMapper;
  private final Clock clock;

  @Override
  public DocumentType type() {
    return DocumentType.ACCOUNT_STATEMENT;
  }

  @Override
  public boolean bankSealCanBeRequired() {
    return true;
  }

  /** The summary only: the counted transactions are never stored. */
  @Override
  public Optional<Object> storedResult(UUID documentId) {
    return summaries
        .findByDocumentId(documentId)
        .map(
            stored ->
                new StatementResult(
                    null,
                    jsonMapper.readValue(stored.getSummary(), StatementSummary.class),
                    List.of()));
  }

  @Override
  public StatementResult process(ReceivedDocument document) {
    DocumentFile pdf =
        document.files().stream()
            .filter(DocumentFile::isPdf)
            .findFirst()
            .orElseThrow(
                () -> new DocumentRejectedException(NO_USABLE_FILE, "No PDF statement found"));
    ExtractedStatement extracted;
    try {
      extracted = extractor.extract(pdf);
    } catch (ExtractionException e) {
      throw new DocumentRejectedException(EXTRACTION_FAILED, e.getMessage(), e);
    }

    var borrower = borrowers.find(document.borrowerId());
    StatementSummary summary =
        analyzer.analyze(
            extracted,
            borrower.map(Borrower::getFirstName).orElse(null),
            borrower.map(Borrower::getLastName).orElse(null));
    summaries.save(
        StoredStatementSummary.of(
            document.id(),
            document.borrowerId(),
            summary,
            jsonMapper.writeValueAsString(summary),
            clock.instant()));

    return new StatementResult(
        mask(extracted.iban()),
        summary,
        extracted.transactions().stream()
            .map(
                t ->
                    new CountedTransaction(
                        t.date(),
                        TransactionCategory.PRIVATE_PAYEE.contains(t.category())
                            ? null
                            : t.counterparty(),
                        t.description(),
                        t.amount(),
                        t.category()))
            .toList());
  }

  static @Nullable String mask(@Nullable String iban) {
    if (iban == null) {
      return null;
    }
    String compact = iban.replaceAll("\\s", "");
    return compact.length() <= 6
        ? compact
        : compact.substring(0, 2) + "…" + compact.substring(compact.length() - 4);
  }
}
