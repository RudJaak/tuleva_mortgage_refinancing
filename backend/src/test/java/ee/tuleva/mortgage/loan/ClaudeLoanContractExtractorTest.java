package ee.tuleva.mortgage.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.models.messages.StructuredOutputConfig;
import ee.tuleva.mortgage.extraction.ExtractionException;
import ee.tuleva.mortgage.loan.ClaudeLoanContractExtractor.ClaudeEvidence;
import ee.tuleva.mortgage.loan.ClaudeLoanContractExtractor.ClaudeLoanContract;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Confidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClaudeLoanContractExtractorTest {

  @Test
  void theOutputSchemaIsAcceptedByTheSdk() {
    var config =
        StructuredOutputConfig.<ClaudeLoanContract>builder()
            .format(ClaudeLoanContract.class)
            .build();

    assertThat(config.format()).isPresent();
  }

  @Test
  void thePromptNamesEveryField() {
    assertThat(ClaudeLoanContractExtractor.SYSTEM_PROMPT)
        .contains(Arrays.stream(LoanField.values()).map(Enum::name).toList());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "185 000,00 | 185000.00",
        "1,95% | 1.95",
        "920.00 € | 920.00",
        "1.234,56 | 1234.56",
        "1,234.56 | 1234.56",
        "185 000 eurot | 185000",
        "222 000 EUR | 222000"
      })
  void readsPrintedAmounts(String printed, String expected) {
    assertThat(ClaudeLoanContractExtractor.amount(printed)).isEqualByComparingTo(expected);
  }

  @Test
  void anEmptyAmountIsUnknown() {
    assertThat(ClaudeLoanContractExtractor.amount(" ")).isNull();
  }

  @Test
  void mapsTheResultWithItsEvidence() {
    var extracted =
        ClaudeLoanContractExtractor.toExtracted(
            result(
                "1,95",
                "2049-06-14",
                List.of(
                    new ClaudeEvidence(LoanField.MARGIN, 2, "Marginaal 1,95%", Confidence.HIGH),
                    new ClaudeEvidence(LoanField.MARGIN, 7, "duplicate", Confidence.LOW))));

    assertThat(extracted.margin()).isEqualByComparingTo("1.95");
    assertThat(extracted.maturityDate()).isEqualTo(LocalDate.of(2049, 6, 14));
    assertThat(extracted.lender()).isEqualTo("Swedbank AS");
    assertThat(extracted.contractNumber()).isNull();
    assertThat(extracted.borrowers()).containsExactly("Mari Maasikas");
    assertThat(extracted.termMonths()).isEqualTo(360);
    assertThat(extracted.evidence().get(LoanField.MARGIN).page()).isEqualTo(2);
  }

  @Test
  void anUnparseableValueFailsTheExtraction() {
    assertThatThrownBy(
            () -> ClaudeLoanContractExtractor.toExtracted(result("1,95", "14.06.2049", List.of())))
        .isInstanceOf(ExtractionException.class);
  }

  private static ClaudeLoanContract result(
      String margin, String maturity, List<ClaudeEvidence> evidence) {
    return new ClaudeLoanContract(
        "Swedbank AS",
        "",
        "2019-06-14",
        List.of("Mari Maasikas", " "),
        "185 000,00",
        "EUR",
        "6 kuu EURIBOR",
        margin,
        RateType.VARIABLE,
        "",
        "",
        maturity,
        "360",
        "920,00",
        "14",
        RepaymentType.ANNUITY,
        "222 000,00",
        "Tallinn",
        "3 kuu intress",
        "",
        "",
        evidence);
  }
}
