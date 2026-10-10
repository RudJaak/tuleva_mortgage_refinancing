package ee.tuleva.mortgage.loan;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.models.messages.StructuredOutputConfig;
import ee.tuleva.mortgage.loan.ClaudeLoanContractExtractor.ClaudeEvidence;
import ee.tuleva.mortgage.loan.ClaudeLoanContractExtractor.ClaudeLoanContract;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Confidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClaudeLoanContractExtractorTest {

  @Test
  void theOutputSchemaIsTypedSoTheApiGuaranteesNumbersAndDates() {
    var schema =
        StructuredOutputConfig.<ClaudeLoanContract>builder()
            .format(ClaudeLoanContract.class)
            .build()
            .format()
            .orElseThrow()
            .schema()
            .toString();

    assertThat(schema)
        .contains("margin={type=[number, null]")
        .contains("termMonths={type=[integer, null]")
        .contains("maturityDate={type=[string, null], format=date");
    assertThat(unionFields(schema)).as("the API's limit").isLessThanOrEqualTo(16);
  }

  static long unionFields(String schema) {
    return java.util.regex.Pattern.compile("type=\\[\\w+, null\\]")
        .matcher(schema)
        .results()
        .count();
  }

  @Test
  void thePromptNamesEveryFieldAndTheNormalForms() {
    assertThat(ClaudeLoanContractExtractor.SYSTEM_PROMPT)
        .contains(Arrays.stream(LoanField.values()).map(Enum::name).toList())
        .contains("82990 for \"82 990 eurot\"")
        .contains("2.95 for \"2,95% aastas\"")
        .contains("Use null for a number or date");
  }

  @Test
  void mapsTheResultWithItsEvidence() {
    var extracted =
        ClaudeLoanContractExtractor.toExtracted(
            result(
                "2.95",
                List.of(
                    new ClaudeEvidence(
                        LoanField.MARGIN, "leping.pdf", 2, "Marginaal 2,95%", Confidence.HIGH),
                    new ClaudeEvidence(
                        LoanField.MARGIN, "teabeleht.pdf", 7, "duplicate", Confidence.LOW))));

    assertThat(extracted.margin()).isEqualByComparingTo("2.95");
    assertThat(extracted.maturityDate()).isEqualTo(LocalDate.of(2045, 3, 5));
    assertThat(extracted.lender()).isEqualTo("Swedbank AS");
    assertThat(extracted.contractNumber()).as("not stated").isNull();
    assertThat(extracted.borrowers()).containsExactly("Mari Maasikas");
    assertThat(extracted.termMonths()).isEqualTo(300);
    assertThat(extracted.propertyValue()).isEqualByComparingTo("162000");
    assertThat(extracted.evidence().get(LoanField.MARGIN).page()).isEqualTo(2);
    assertThat(extracted.evidence().get(LoanField.MARGIN).document()).isEqualTo("leping.pdf");
  }

  @Test
  void aMisreadNumberOutsideItsPlausibleRangeIsLeftEmpty() {
    // "2,95%" read as 295
    var extracted = ClaudeLoanContractExtractor.toExtracted(result("295", List.of()));

    assertThat(extracted.margin()).isNull();
    assertThat(extracted.principal()).isEqualByComparingTo("82990");
  }

  @Test
  void aDocumentCitedByItsHeadingIsMappedToItsFile() {
    var titles = List.of("EAL-20200305XY Mari Maasikas.pdf", "Standardinfo teabeleht.pdf");

    assertThat(ClaudeLoanContractExtractor.closestTitle("Laenuleping nr EAL-20200305XY", titles))
        .isEqualTo("EAL-20200305XY Mari Maasikas.pdf");
    assertThat(ClaudeLoanContractExtractor.closestTitle("Standardinfo teabeleht.pdf", titles))
        .isEqualTo("Standardinfo teabeleht.pdf");
  }

  private static ClaudeLoanContract result(String margin, List<ClaudeEvidence> evidence) {
    return new ClaudeLoanContract(
        "Swedbank AS",
        " ",
        Optional.of(LocalDate.of(2020, 3, 5)),
        List.of("Mari Maasikas", " "),
        Optional.of(new BigDecimal("82990")),
        "EUR",
        "6 kuu Euribor",
        Optional.of(new BigDecimal(margin)),
        RateType.VARIABLE,
        Optional.empty(),
        Optional.empty(),
        Optional.of(LocalDate.of(2045, 3, 5)),
        Optional.of(300),
        Optional.of(new BigDecimal("393.14")),
        Optional.of(5),
        RepaymentType.ANNUITY,
        Optional.of(new BigDecimal("107887")),
        "Tallinn",
        "Tasu ei rakendata",
        Optional.of(new BigDecimal("162000")),
        Optional.empty(),
        Optional.empty(),
        evidence);
  }
}
