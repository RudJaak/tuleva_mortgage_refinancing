package ee.tuleva.mortgage.loan;

import static ee.tuleva.mortgage.loan.ExtractedLoanContract.Confidence.HIGH;
import static ee.tuleva.mortgage.loan.ExtractedLoanContract.Confidence.MEDIUM;

import ee.tuleva.mortgage.document.DocumentFile;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.Evidence;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Offline stand-in for the LLM: the same synthetic contract for any file. The borrowers are left
 * out, so the party check reports NOT_CHECKED instead of a false mismatch.
 */
class StubLoanContractExtractor implements LoanContractExtractor {

  private static final String CONTRACT = "laenuleping.pdf";

  @Override
  public ExtractedLoanContract extract(List<DocumentFile> pdfs) {
    return new ExtractedLoanContract(
        "Swedbank AS",
        "KL-2019-000123",
        LocalDate.of(2019, 6, 14),
        List.of(),
        new BigDecimal("185000.00"),
        "EUR",
        "6 kuu EURIBOR",
        new BigDecimal("1.950"),
        ExtractedLoanContract.RateType.VARIABLE,
        null,
        null,
        LocalDate.of(2049, 6, 14),
        360,
        new BigDecimal("920.00"),
        14,
        ExtractedLoanContract.RepaymentType.ANNUITY,
        new BigDecimal("222000.00"),
        "Tallinn, Näidise tn 1-12",
        "Hüvitis kuni 3 kuu intressi ulatuses",
        new BigDecimal("231000.00"),
        null,
        null,
        Map.of(
            LoanField.LENDER, new Evidence(CONTRACT, 1, "Laenuandja: Swedbank AS", HIGH),
            LoanField.PRINCIPAL, new Evidence(CONTRACT, 2, "Laenusumma 185 000,00 eurot", HIGH),
            LoanField.MARGIN, new Evidence(CONTRACT, 2, "Marginaal 1,95% aastas", HIGH),
            LoanField.BASE_RATE, new Evidence(CONTRACT, 2, "Baasintress 6 kuu EURIBOR", HIGH),
            LoanField.MATURITY_DATE,
                new Evidence(CONTRACT, 2, "Tagastamise tähtpäev 14.06.2049", HIGH),
            LoanField.MONTHLY_PAYMENT,
                new Evidence(CONTRACT, 3, "Annuiteetmakse 920,00 eurot", MEDIUM),
            LoanField.EARLY_REPAYMENT_TERMS,
                new Evidence(CONTRACT, 5, "hüvitist kuni 3 kuu intressi ulatuses", HIGH),
            LoanField.PROPERTY_VALUE,
                new Evidence(
                    "teabeleht.pdf", 2, "kinnisvara eeldatav väärtus: 231 000 eurot", HIGH)));
  }
}
