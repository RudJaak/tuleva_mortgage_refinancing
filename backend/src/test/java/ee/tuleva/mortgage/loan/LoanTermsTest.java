package ee.tuleva.mortgage.loan;

import static ee.tuleva.mortgage.loan.LoanTerms.Source.DECLARED;
import static ee.tuleva.mortgage.loan.LoanTerms.Source.DOCUMENT;
import static org.assertj.core.api.Assertions.assertThat;

import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoanTermsTest {

  private final ExtractedLoanContract read = new StubLoanContractExtractor().extract(List.of());

  @Test
  void confirmedValuesComeFromTheDocumentAndCorrectionsAreDeclared() {
    var terms = terms(new BigDecimal("1.95"), LocalDate.of(2049, 6, 14), new BigDecimal("150000"));

    var provenance = terms.provenance(read);

    assertThat(provenance)
        .containsEntry(LoanField.LENDER, DOCUMENT)
        .as("1.95 equals the 1.950 read")
        .containsEntry(LoanField.MARGIN, DOCUMENT)
        .containsEntry(LoanField.MATURITY_DATE, DOCUMENT)
        .containsEntry(LoanField.BALANCE, DECLARED)
        .doesNotContainKey(LoanField.FIXED_RATE_UNTIL);
  }

  @Test
  void withoutAContractEverythingIsDeclared() {
    var provenance = terms(new BigDecimal("1.95"), null, null).provenance(null);

    assertThat(provenance)
        .containsEntry(LoanField.LENDER, DECLARED)
        .containsEntry(LoanField.MARGIN, DECLARED);
  }

  @Test
  void theTermIsKeptOnlyWhileTheMaturityIsUnchanged() {
    var today = LocalDate.of(2026, 10, 15);

    var unchanged =
        terms(new BigDecimal("1.95"), LocalDate.of(2049, 6, 14), null).toContract(read, today);
    var moved =
        terms(new BigDecimal("1.95"), LocalDate.of(2044, 6, 14), null).toContract(read, today);

    assertThat(unchanged.termMonths()).isEqualTo(360);
    assertThat(moved.termMonths()).isNull();
    assertThat(moved.evidence()).isEqualTo(read.evidence());
  }

  @Test
  void aStatedBalanceIsDatedToday() {
    var today = LocalDate.of(2026, 10, 15);

    var contract =
        terms(new BigDecimal("1.95"), null, new BigDecimal("150000")).toContract(read, today);

    assertThat(contract.balanceDate()).isEqualTo(today);
  }

  private static LoanTerms terms(BigDecimal margin, LocalDate maturity, BigDecimal balance) {
    return new LoanTerms(
        null,
        "Swedbank AS",
        "KL-2019-000123",
        LocalDate.of(2019, 6, 14),
        new BigDecimal("185000"),
        margin,
        "6 kuu EURIBOR",
        RateType.VARIABLE,
        null,
        maturity,
        new BigDecimal("920"),
        RepaymentType.ANNUITY,
        balance,
        new BigDecimal("231000"));
  }
}
