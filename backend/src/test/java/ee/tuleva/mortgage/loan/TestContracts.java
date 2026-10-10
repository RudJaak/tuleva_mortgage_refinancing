package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.loan.ExtractedLoanContract.Evidence;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RateType;
import ee.tuleva.mortgage.loan.ExtractedLoanContract.RepaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Readable test contracts: start empty and set only what a test is about. */
final class TestContracts {

  String lender = "SEB";
  LocalDate contractDate;
  List<String> borrowers = List.of();
  BigDecimal principal;
  BigDecimal margin;
  RateType rateType = RateType.VARIABLE;
  LocalDate fixedRateUntil;
  BigDecimal statedInterestRate;
  LocalDate maturityDate;
  Integer termMonths;
  BigDecimal monthlyPayment;
  RepaymentType repaymentType = RepaymentType.ANNUITY;
  BigDecimal propertyValue;
  BigDecimal balance;
  Map<LoanField, Evidence> evidence = Map.of();

  static TestContracts contract() {
    return new TestContracts();
  }

  TestContracts lender(String value) {
    lender = value;
    return this;
  }

  TestContracts signed(String date) {
    contractDate = LocalDate.parse(date);
    return this;
  }

  TestContracts borrowers(String... names) {
    borrowers = List.of(names);
    return this;
  }

  TestContracts principal(String value) {
    principal = new BigDecimal(value);
    return this;
  }

  TestContracts margin(String value) {
    margin = value == null ? null : new BigDecimal(value);
    return this;
  }

  TestContracts fixedUntil(String date, String rate) {
    rateType = RateType.MIXED;
    fixedRateUntil = LocalDate.parse(date);
    statedInterestRate = new BigDecimal(rate);
    return this;
  }

  TestContracts maturity(String date) {
    maturityDate = LocalDate.parse(date);
    return this;
  }

  TestContracts term(int months) {
    termMonths = months;
    return this;
  }

  TestContracts payment(String value) {
    monthlyPayment = new BigDecimal(value);
    return this;
  }

  TestContracts equalPrincipal() {
    repaymentType = RepaymentType.EQUAL_PRINCIPAL;
    return this;
  }

  TestContracts propertyValue(String value) {
    propertyValue = new BigDecimal(value);
    return this;
  }

  TestContracts balance(String value) {
    balance = new BigDecimal(value);
    return this;
  }

  TestContracts unknownRate() {
    rateType = RateType.UNKNOWN;
    repaymentType = RepaymentType.UNKNOWN;
    return this;
  }

  ExtractedLoanContract build() {
    return new ExtractedLoanContract(
        lender,
        null,
        contractDate,
        borrowers,
        principal,
        "EUR",
        "6 kuu EURIBOR",
        margin,
        rateType,
        fixedRateUntil,
        statedInterestRate,
        maturityDate,
        termMonths,
        monthlyPayment,
        null,
        repaymentType,
        null,
        null,
        null,
        propertyValue,
        balance,
        balance == null ? null : LocalDate.of(2026, 10, 1),
        evidence);
  }
}
