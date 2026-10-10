package ee.tuleva.mortgage.loan;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.extraction.ClaudeDocumentReader;
import ee.tuleva.mortgage.extraction.ExtractionException;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Confidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Evidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Claude reads the contract and returns each field as printed, with the page and quote it came
 * from. Values come back as text and are parsed here, so Estonian number formats ("185 000,00",
 * "1,95%") are handled in one tested place.
 */
@RequiredArgsConstructor
class ClaudeLoanContractExtractor implements LoanContractExtractor {

  static final String SYSTEM_PROMPT =
      """
      You read Estonian home loan contracts (kodulaenu leping) for a refinancing pre-screening. \
      The borrower uploaded their own contract and asked us to read it.

      Fill in these fields:

      %s

      Rules:
      - Copy values as printed. Dates as YYYY-MM-DD. Leave a field empty when the contract does \
      not state it; never calculate or guess a value that is not written down.
      - The contract may include amendments or annexes (muudatus, lisa). Return the terms in \
      force after the latest amendment, and quote the amendment as the evidence.
      - Read the loan's own terms, not the bank's general conditions or examples in a price list.
      - For every field you fill, add one evidence entry: the 1-based PDF page, a short verbatim \
      quote (at most 150 characters) and your confidence. Use LOW when the wording is ambiguous \
      or the value is hard to read.\
      """
          .formatted(
              Arrays.stream(LoanField.values())
                  .map(f -> "- " + f.name() + ": " + f.description())
                  .collect(Collectors.joining("\n")));

  record ClaudeLoanContract(
      String lender,
      String contractNumber,
      @JsonPropertyDescription("YYYY-MM-DD") String contractDate,
      @JsonPropertyDescription("Full names of all borrowers") List<String> borrowers,
      @JsonPropertyDescription("As printed, e.g. '185 000,00'") String principal,
      String currency,
      String baseRate,
      @JsonPropertyDescription("Percent per year as printed, e.g. '1,95'") String margin,
      RateType rateType,
      @JsonPropertyDescription("YYYY-MM-DD") String fixedRateUntil,
      @JsonPropertyDescription("Percent per year as printed") String statedInterestRate,
      @JsonPropertyDescription("YYYY-MM-DD") String maturityDate,
      String termMonths,
      @JsonPropertyDescription("As printed") String monthlyPayment,
      String paymentDay,
      RepaymentType repaymentType,
      @JsonPropertyDescription("As printed") String mortgageAmount,
      String collateral,
      String earlyRepaymentTerms,
      @JsonPropertyDescription("Outstanding principal as printed, if stated") String balance,
      @JsonPropertyDescription("YYYY-MM-DD of the stated balance") String balanceDate,
      List<ClaudeEvidence> evidence) {}

  record ClaudeEvidence(
      LoanField field,
      @JsonPropertyDescription("1-based PDF page") int page,
      @JsonPropertyDescription("Verbatim, at most 150 characters") String quote,
      Confidence confidence) {}

  private final ClaudeDocumentReader reader;

  @Override
  public ExtractedLoanContract extract(DocumentFile contract) {
    return toExtracted(
        reader.read(
            contract.content(),
            SYSTEM_PROMPT,
            "Extract this home loan contract.",
            ClaudeLoanContract.class));
  }

  static ExtractedLoanContract toExtracted(ClaudeLoanContract result) {
    var evidence = new EnumMap<LoanField, Evidence>(LoanField.class);
    result
        .evidence()
        .forEach(
            e ->
                evidence.putIfAbsent(e.field(), new Evidence(e.page(), e.quote(), e.confidence())));
    try {
      return new ExtractedLoanContract(
          text(result.lender()),
          text(result.contractNumber()),
          date(result.contractDate()),
          result.borrowers().stream().filter(b -> !b.isBlank()).map(String::strip).toList(),
          amount(result.principal()),
          text(result.currency()),
          text(result.baseRate()),
          amount(result.margin()),
          result.rateType(),
          date(result.fixedRateUntil()),
          amount(result.statedInterestRate()),
          date(result.maturityDate()),
          integer(result.termMonths()),
          amount(result.monthlyPayment()),
          integer(result.paymentDay()),
          result.repaymentType(),
          amount(result.mortgageAmount()),
          text(result.collateral()),
          text(result.earlyRepaymentTerms()),
          amount(result.balance()),
          date(result.balanceDate()),
          evidence);
    } catch (DateTimeParseException | NumberFormatException | ArithmeticException e) {
      throw new ExtractionException("The contract reader returned a value it could not parse", e);
    }
  }

  /** Parses Estonian and international number formats: "185 000,00", "1,95%", "920.00 €". */
  static @Nullable BigDecimal amount(@Nullable String printed) {
    if (printed == null || printed.isBlank()) {
      return null;
    }
    String digits = printed.replaceAll("[\\s\\u00a0€%]|EUR|eurot?", "");
    if (digits.contains(",") && digits.contains(".")) {
      digits =
          digits.lastIndexOf(',') > digits.lastIndexOf('.')
              ? digits.replace(".", "").replace(',', '.')
              : digits.replace(",", "");
    } else {
      digits = digits.replace(',', '.');
    }
    return new BigDecimal(digits);
  }

  private static @Nullable Integer integer(@Nullable String printed) {
    BigDecimal value = amount(printed);
    return value == null ? null : value.intValueExact();
  }

  private static @Nullable LocalDate date(@Nullable String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value.strip());
  }

  private static @Nullable String text(@Nullable String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
