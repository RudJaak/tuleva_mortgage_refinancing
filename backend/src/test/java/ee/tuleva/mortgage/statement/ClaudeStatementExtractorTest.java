package ee.tuleva.mortgage.statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.models.messages.StructuredOutputConfig;
import ee.tuleva.mortgage.statement.ClaudeStatementExtractor.ClaudeStatement;
import ee.tuleva.mortgage.statement.ClaudeStatementExtractor.ClaudeTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClaudeStatementExtractorTest {

  @Test
  void theOutputSchemaIsAcceptedByTheSdk() {
    var config =
        StructuredOutputConfig.<ClaudeStatement>builder().format(ClaudeStatement.class).build();

    assertThat(config.format()).isPresent();
  }

  @Test
  void thePromptNamesEveryCategory() {
    assertThat(ClaudeStatementExtractor.SYSTEM_PROMPT)
        .contains(Arrays.stream(TransactionCategory.values()).map(Enum::name).toList());
  }

  @Test
  void mapsTheResultAndTurnsBlanksIntoUnknowns() {
    var result =
        new ClaudeStatement(
            " ",
            "",
            "2026-04-10",
            "",
            new BigDecimal("10.00"),
            new BigDecimal("20.00"),
            new BigDecimal("-5.00"),
            List.of(
                new ClaudeTransaction(
                    "2026-04-30",
                    "Firma OÜ",
                    "Töötasu",
                    new BigDecimal("2000.00"),
                    TransactionCategory.SALARY)));

    var extracted = ClaudeStatementExtractor.toExtracted(result);

    assertThat(extracted.accountHolder()).isNull();
    assertThat(extracted.iban()).isNull();
    assertThat(extracted.periodStart()).isEqualTo(LocalDate.of(2026, 4, 10));
    assertThat(extracted.periodEnd()).isNull();
    assertThat(extracted.lowestBalance()).isEqualByComparingTo("-5.00");
    assertThat(extracted.transactions())
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.date()).isEqualTo(LocalDate.of(2026, 4, 30));
              assertThat(t.category()).isEqualTo(TransactionCategory.SALARY);
            });
  }

  @Test
  void anUnreadableDateFailsTheExtraction() {
    var result =
        new ClaudeStatement(
            "", "", "10.04.2026", "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());

    assertThatThrownBy(() -> ClaudeStatementExtractor.toExtracted(result))
        .isInstanceOf(ee.tuleva.mortgage.extraction.ExtractionException.class);
  }
}
