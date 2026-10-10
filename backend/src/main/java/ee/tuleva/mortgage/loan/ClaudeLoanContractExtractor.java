package ee.tuleva.mortgage.loan;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.extraction.ClaudeDocumentReader;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Confidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Evidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Claude reads the contract upload and returns each field normalised: amounts and percentages as
 * numbers, dates as dates, null when not stated (texts: empty). The output schema is typed, so the
 * API guarantees those shapes; the API allows at most 16 nullable fields, so texts are not
 * nullable; the contract's own wording goes into each field's evidence quote. Values outside a
 * plausible range (a misread "2,95%" as 295) are dropped and left for the borrower to fill in.
 */
@Slf4j
@RequiredArgsConstructor
class ClaudeLoanContractExtractor implements LoanContractExtractor {

  static final String SYSTEM_PROMPT =
      """
      You read Estonian home loan contracts (kodulaenu leping) for a refinancing pre-screening. \
      The borrower uploaded their own contract and asked us to read it. The upload can hold \
      several documents: the contract, often with the European standard information sheet \
      (Euroopa tarbijakrediidi standardinfo teabeleht, ESIS), a repayment schedule (maksegraafik) \
      or the bank's general conditions.

      Fill in these fields:

      %s

      Rules:
      - Give every value in its normal form, as each field's description says: amounts in \
      euros and percentages as plain numbers with a dot as the decimal separator and no spaces, \
      units or currency (82990 for "82 990 eurot", 2.95 for "2,95%% aastas"); dates as dates; \
      whole numbers for counts and days.
      - Use null for a number or date the documents do not state, and an empty string for such a \
      text. Never calculate or guess a value that is not written down; converting a term in \
      years to months is fine.
      - The contract may include amendments or annexes (muudatus, lisa). Return the terms in \
      force after the latest amendment, and quote the amendment as the evidence.
      - The contract wins when documents disagree. Use the information sheet or the schedule for \
      what the contract leaves out, e.g. the monthly payment, the base rate's tenor or the \
      property value.
      - Read the loan's own terms, not the general conditions' definitions or price-list examples.
      - For every field you fill, add one evidence entry: the document's title, the 1-based page \
      within that document, a short verbatim quote of the original wording (at most 150 \
      characters) and your confidence. Use LOW when the wording is ambiguous or the value is hard \
      to read.\
      """
          .formatted(
              Arrays.stream(LoanField.values())
                  .map(f -> "- " + f.name() + ": " + f.description())
                  .collect(Collectors.joining("\n")));

  record ClaudeLoanContract(
      @JsonPropertyDescription("The lending bank's name, e.g. 'AS LHV Pank'") String lender,
      @JsonPropertyDescription("Contract number as printed") String contractNumber,
      @JsonPropertyDescription("Date the contract was signed") Optional<LocalDate> contractDate,
      @JsonPropertyDescription("Full names of all borrowers; empty if none are named")
          List<String> borrowers,
      @JsonPropertyDescription("Loan amount in euros, e.g. 82990 for '82 990 eurot'")
          Optional<BigDecimal> principal,
      @JsonPropertyDescription("ISO currency code, e.g. EUR") String currency,
      @JsonPropertyDescription("Base rate with its tenor, e.g. '6 kuu Euribor'") String baseRate,
      @JsonPropertyDescription("Margin in percent per year, e.g. 2.95 for '2,95% aastas'")
          Optional<BigDecimal> margin,
      RateType rateType,
      @JsonPropertyDescription("End of a fixed-rate period") Optional<LocalDate> fixedRateUntil,
      @JsonPropertyDescription(
              "Total interest rate in percent per year, if the contract states one")
          Optional<BigDecimal> statedInterestRate,
      @JsonPropertyDescription("Final repayment date") Optional<LocalDate> maturityDate,
      @JsonPropertyDescription(
              "Loan term in months, e.g. 300 for '300 osamakset' or for '25 aastat'")
          Optional<Integer> termMonths,
      @JsonPropertyDescription("Monthly instalment in euros, e.g. 393.14")
          Optional<BigDecimal> monthlyPayment,
      @JsonPropertyDescription("Day of the month payments are due, 1 to 31")
          Optional<Integer> paymentDay,
      RepaymentType repaymentType,
      @JsonPropertyDescription("Mortgage (hypothec) amount in euros")
          Optional<BigDecimal> mortgageAmount,
      @JsonPropertyDescription("The mortgaged property: address and land register number")
          String collateral,
      @JsonPropertyDescription("Early repayment fee or compensation, in a short sentence")
          String earlyRepaymentTerms,
      @JsonPropertyDescription("Value of the mortgaged property in euros")
          Optional<BigDecimal> propertyValue,
      @JsonPropertyDescription("Outstanding principal in euros, if stated")
          Optional<BigDecimal> balance,
      @JsonPropertyDescription("Date of the stated outstanding principal")
          Optional<LocalDate> balanceDate,
      List<ClaudeEvidence> evidence) {}

  record ClaudeEvidence(
      LoanField field,
      @JsonPropertyDescription("Title of the document the value was read from") String document,
      @JsonPropertyDescription("1-based page within that document") int page,
      @JsonPropertyDescription("The original wording, verbatim, at most 150 characters")
          String quote,
      Confidence confidence) {}

  /** A value outside these bounds is a misreading, not a loan term. */
  private record Range(BigDecimal min, BigDecimal max) {

    static Range of(String min, String max) {
      return new Range(new BigDecimal(min), new BigDecimal(max));
    }

    boolean contains(BigDecimal value) {
      return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
  }

  private static final Range PERCENT = Range.of("0", "15");
  private static final Range EUROS = Range.of("1", "10000000");
  private static final Range MONTHS = Range.of("1", "600");
  private static final Range DAY_OF_MONTH = Range.of("1", "31");

  private final ClaudeDocumentReader reader;
  private final Clock clock;

  @Override
  public ExtractedLoanContract extract(List<DocumentFile> pdfs) {
    var result =
        reader.read(
            pdfs.stream()
                .map(pdf -> new ClaudeDocumentReader.Pdf(pdf.name(), pdf.content()))
                .toList(),
            SYSTEM_PROMPT,
            "Extract this home loan contract. Today is " + LocalDate.now(clock) + ".",
            ClaudeLoanContract.class);
    return toExtracted(result, pdfs.stream().map(DocumentFile::name).toList());
  }

  static ExtractedLoanContract toExtracted(ClaudeLoanContract result) {
    return toExtracted(result, List.of());
  }

  static ExtractedLoanContract toExtracted(ClaudeLoanContract result, List<String> titles) {
    var evidence = new EnumMap<LoanField, Evidence>(LoanField.class);
    result
        .evidence()
        .forEach(
            e ->
                evidence.putIfAbsent(
                    e.field(),
                    new Evidence(
                        closestTitle(e.document(), titles), e.page(), e.quote(), e.confidence())));
    return new ExtractedLoanContract(
        text(result.lender()),
        text(result.contractNumber()),
        result.contractDate().orElse(null),
        result.borrowers().stream().filter(b -> !b.isBlank()).map(String::strip).toList(),
        within(LoanField.PRINCIPAL, result.principal(), EUROS),
        text(result.currency()),
        text(result.baseRate()),
        within(LoanField.MARGIN, result.margin(), PERCENT),
        result.rateType(),
        result.fixedRateUntil().orElse(null),
        within(LoanField.STATED_INTEREST_RATE, result.statedInterestRate(), PERCENT),
        result.maturityDate().orElse(null),
        whole(LoanField.TERM_MONTHS, result.termMonths(), MONTHS),
        within(LoanField.MONTHLY_PAYMENT, result.monthlyPayment(), EUROS),
        whole(LoanField.PAYMENT_DAY, result.paymentDay(), DAY_OF_MONTH),
        result.repaymentType(),
        within(LoanField.MORTGAGE_AMOUNT, result.mortgageAmount(), EUROS),
        text(result.collateral()),
        text(result.earlyRepaymentTerms()),
        within(LoanField.PROPERTY_VALUE, result.propertyValue(), EUROS),
        within(LoanField.BALANCE, result.balance(), EUROS),
        result.balanceDate().orElse(null),
        evidence);
  }

  /** A number outside its plausible range is dropped; only the field name is logged. */
  private static @Nullable BigDecimal within(
      LoanField field, Optional<BigDecimal> value, Range range) {
    if (value.isPresent() && !range.contains(value.get())) {
      log.info("Contract field {} out of range, left for the borrower to fill in", field);
      return null;
    }
    return value.orElse(null);
  }

  private static @Nullable Integer whole(LoanField field, Optional<Integer> value, Range range) {
    BigDecimal checked = within(field, value.map(BigDecimal::valueOf), range);
    return checked == null ? null : checked.intValue();
  }

  /**
   * Claude sometimes cites a document by its heading ("Laenuleping nr …") instead of the title it
   * was given; this maps the citation back to the upload's file with the most words in common.
   */
  static String closestTitle(String cited, List<String> titles) {
    if (titles.isEmpty() || titles.contains(cited)) {
      return cited;
    }
    Set<String> citedWords = words(cited);
    return titles.stream()
        .max(Comparator.comparingLong(t -> words(t).stream().filter(citedWords::contains).count()))
        .orElse(cited);
  }

  private static Set<String> words(String text) {
    return Arrays.stream(text.toLowerCase().replace(".pdf", "").split("[^\\p{L}\\p{N}]+"))
        .filter(word -> word.length() >= 3)
        .collect(Collectors.toSet());
  }

  private static @Nullable String text(@Nullable String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
