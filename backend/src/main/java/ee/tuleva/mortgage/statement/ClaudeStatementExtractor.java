package ee.tuleva.mortgage.statement;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.extraction.ClaudeDocumentReader;
import ee.tuleva.mortgage.extraction.ExtractionException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Claude reads the statement PDF and returns only the transactions that matter for screening,
 * already categorised. Totals and averages are left to {@link StatementAnalyzer}.
 */
@RequiredArgsConstructor
class ClaudeStatementExtractor implements StatementExtractor {

  static final String SYSTEM_PROMPT =
      """
      You read Estonian bank account statements for a mortgage refinancing pre-screening. \
      The statement is the borrower's own and they asked us to analyse it.

      Return the statement's frame (holder, IBAN, period, opening, closing and lowest running \
      balance) and only the transactions that fall into one of these categories:

      %s

      Leave out everything else: card purchases, utilities, transfers between the holder's own \
      accounts, refunds. Copy dates as YYYY-MM-DD and amounts exactly as printed, signed: \
      credits positive, debits negative. Use the statement's own text for counterparty and \
      description. If a field is not on the statement, return an empty string, or 0 for an \
      amount. Never guess a category: when a transaction fits none, leave it out.\
      """
          .formatted(
              Arrays.stream(TransactionCategory.values())
                  .map(c -> "- " + c.name() + ": " + c.description())
                  .collect(Collectors.joining("\n")));

  record ClaudeStatement(
      @JsonPropertyDescription("Account holder's name as printed, or empty") String accountHolder,
      @JsonPropertyDescription("Account IBAN, or empty") String iban,
      @JsonPropertyDescription("First day of the statement period, YYYY-MM-DD") String periodStart,
      @JsonPropertyDescription("Last day of the statement period, YYYY-MM-DD") String periodEnd,
      @JsonPropertyDescription("Opening balance (algsaldo)") BigDecimal openingBalance,
      @JsonPropertyDescription("Closing balance (lõppsaldo)") BigDecimal closingBalance,
      @JsonPropertyDescription("Lowest running balance during the period") BigDecimal lowestBalance,
      @JsonPropertyDescription("Only the transactions in one of the listed categories")
          List<ClaudeTransaction> transactions) {}

  record ClaudeTransaction(
      @JsonPropertyDescription("Booking date, YYYY-MM-DD") String date,
      @JsonPropertyDescription("Payer or payee as printed") String counterparty,
      @JsonPropertyDescription("Explanation or reference as printed") String description,
      @JsonPropertyDescription("Signed amount: credits positive, debits negative")
          BigDecimal amount,
      TransactionCategory category) {}

  private final ClaudeDocumentReader reader;

  @Override
  public ExtractedStatement extract(DocumentFile statement) {
    return toExtracted(
        reader.read(
            statement.content(),
            SYSTEM_PROMPT,
            "Extract this account statement.",
            ClaudeStatement.class));
  }

  static ExtractedStatement toExtracted(ClaudeStatement result) {
    try {
      return new ExtractedStatement(
          blankToNull(result.accountHolder()),
          blankToNull(result.iban()),
          date(result.periodStart()),
          date(result.periodEnd()),
          result.openingBalance(),
          result.closingBalance(),
          result.lowestBalance(),
          result.transactions().stream()
              .map(
                  t ->
                      new ExtractedStatement.Transaction(
                          LocalDate.parse(t.date()),
                          t.counterparty(),
                          t.description(),
                          t.amount(),
                          t.category()))
              .toList());
    } catch (DateTimeParseException e) {
      throw new ExtractionException("The statement reader returned an invalid date", e);
    }
  }

  private static @Nullable LocalDate date(@Nullable String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value);
  }

  private static @Nullable String blankToNull(@Nullable String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
