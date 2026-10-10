package ee.tuleva.mortgage.statement;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.extraction.ClaudeDocumentReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Claude reads the statement PDF and returns only the transactions that matter for screening,
 * already categorised. The output schema is typed (dates as dates, amounts as numbers, null when
 * not shown), so the API guarantees those shapes. Totals and averages are left to {@link
 * StatementAnalyzer}.
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
      accounts, refunds. Give amounts in euros as plain numbers with a dot as the decimal \
      separator and no spaces or currency, signed: credits positive, debits negative (-120.5 for \
      "-120,50"). Give dates as dates. Use the statement's own text for counterparty and \
      description. Use null when the statement does not show a value; never use 0 for an unknown \
      balance. Never guess a category: when a transaction fits none, leave it out.\
      """
          .formatted(
              Arrays.stream(TransactionCategory.values())
                  .map(c -> "- " + c.name() + ": " + c.description())
                  .collect(Collectors.joining("\n")));

  record ClaudeStatement(
      @JsonPropertyDescription("Account holder's name as printed") Optional<String> accountHolder,
      @JsonPropertyDescription("Account IBAN") Optional<String> iban,
      @JsonPropertyDescription("First day of the statement period") Optional<LocalDate> periodStart,
      @JsonPropertyDescription("Last day of the statement period") Optional<LocalDate> periodEnd,
      @JsonPropertyDescription("Opening balance (algsaldo) in euros")
          Optional<BigDecimal> openingBalance,
      @JsonPropertyDescription("Closing balance (lõppsaldo) in euros")
          Optional<BigDecimal> closingBalance,
      @JsonPropertyDescription("Lowest running balance during the period, in euros")
          Optional<BigDecimal> lowestBalance,
      @JsonPropertyDescription("Only the transactions in one of the listed categories")
          List<ClaudeTransaction> transactions) {}

  record ClaudeTransaction(
      @JsonPropertyDescription("Booking date") LocalDate date,
      @JsonPropertyDescription("Payer or payee as printed") String counterparty,
      @JsonPropertyDescription("Explanation or reference as printed") String description,
      @JsonPropertyDescription("Signed amount in euros: credits positive, debits negative")
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
    return new ExtractedStatement(
        text(result.accountHolder()),
        text(result.iban()),
        result.periodStart().orElse(null),
        result.periodEnd().orElse(null),
        result.openingBalance().orElse(null),
        result.closingBalance().orElse(null),
        result.lowestBalance().orElse(null),
        result.transactions().stream()
            .map(
                t ->
                    new ExtractedStatement.Transaction(
                        t.date(), t.counterparty(), t.description(), t.amount(), t.category()))
            .toList());
  }

  private static @Nullable String text(Optional<String> value) {
    return value.map(String::strip).filter(v -> !v.isEmpty()).orElse(null);
  }
}
