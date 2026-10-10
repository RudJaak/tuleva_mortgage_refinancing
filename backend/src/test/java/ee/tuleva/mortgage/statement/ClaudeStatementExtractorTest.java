package ee.tuleva.mortgage.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.models.messages.StructuredOutputConfig;
import ee.tuleva.mortgage.statement.ClaudeStatementExtractor.ClaudeStatement;
import ee.tuleva.mortgage.statement.ClaudeStatementExtractor.ClaudeTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClaudeStatementExtractorTest {

  @Test
  void theOutputSchemaIsTypedSoTheApiGuaranteesNumbersAndDates() {
    var schema =
        StructuredOutputConfig.<ClaudeStatement>builder()
            .format(ClaudeStatement.class)
            .build()
            .format()
            .orElseThrow()
            .schema()
            .toString();

    assertThat(schema)
        .contains("lowestBalance={type=[number, null]")
        .contains("periodStart={type=[string, null], format=date");
    assertThat(
            java.util.regex.Pattern.compile("type=\\[\\w+, null\\]")
                .matcher(schema)
                .results()
                .count())
        .as("the API allows at most 16 nullable fields")
        .isLessThanOrEqualTo(16);
  }

  @Test
  void thePromptNamesEveryCategoryAndTheNormalForms() {
    assertThat(ClaudeStatementExtractor.SYSTEM_PROMPT)
        .contains(Arrays.stream(TransactionCategory.values()).map(Enum::name).toList())
        .contains("-120.5 for \"-120,50\"")
        .contains("never use 0 for an unknown");
  }

  @Test
  void mapsTheResultAndKeepsUnknownsAsUnknown() {
    var result =
        new ClaudeStatement(
            Optional.of(" "),
            Optional.empty(),
            Optional.of(LocalDate.of(2026, 4, 10)),
            Optional.empty(),
            Optional.of(new BigDecimal("10.00")),
            Optional.empty(),
            Optional.of(new BigDecimal("-5.00")),
            List.of(
                new ClaudeTransaction(
                    LocalDate.of(2026, 4, 30),
                    "Firma OÜ",
                    "Töötasu",
                    new BigDecimal("2000.00"),
                    TransactionCategory.SALARY)));

    var extracted = ClaudeStatementExtractor.toExtracted(result);

    assertThat(extracted.accountHolder()).isNull();
    assertThat(extracted.iban()).isNull();
    assertThat(extracted.periodStart()).isEqualTo(LocalDate.of(2026, 4, 10));
    assertThat(extracted.periodEnd()).isNull();
    assertThat(extracted.closingBalance()).as("unknown, not 0").isNull();
    assertThat(extracted.lowestBalance()).isEqualByComparingTo("-5.00");
    assertThat(extracted.transactions())
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.date()).isEqualTo(LocalDate.of(2026, 4, 30));
              assertThat(t.category()).isEqualTo(TransactionCategory.SALARY);
            });
  }
}
