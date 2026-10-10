package ee.tuleva.mortgage.loan;

/** The contract fields we read; each comes back with the page and quote it was read from. */
enum LoanField {
  LENDER("The lending bank's name"),
  CONTRACT_NUMBER("Contract number ('lepingu nr')"),
  CONTRACT_DATE("Date the contract was signed"),
  BORROWERS("Every borrower's full name ('laenusaaja'); co-borrowers included, guarantors not"),
  PRINCIPAL("Loan amount granted ('laenusumma')"),
  CURRENCY("Loan currency, usually EUR"),
  BASE_RATE("Base rate the interest floats on, e.g. '6 kuu EURIBOR'"),
  MARGIN("Margin over the base rate in percent per year ('marginaal'), e.g. 1.95"),
  RATE_TYPE("VARIABLE (base rate + margin), FIXED, or MIXED (fixed for a period, then variable)"),
  FIXED_RATE_UNTIL("End of a fixed-rate period, if any"),
  STATED_INTEREST_RATE("Total interest rate if the contract states one ('intressimäär')"),
  MATURITY_DATE("Final repayment date ('lõpptähtpäev', 'tagastamise tähtpäev')"),
  TERM_MONTHS("Loan term in months ('laenuperiood'), if stated"),
  MONTHLY_PAYMENT("Monthly instalment of principal and interest ('kuumakse', 'annuiteet')"),
  PAYMENT_DAY("Day of the month payments are due ('maksepäev')"),
  REPAYMENT_TYPE("ANNUITY (equal instalments) or EQUAL_PRINCIPAL ('võrdsed põhiosamaksed')"),
  MORTGAGE_AMOUNT("Mortgage (hypothec) amount securing the loan ('hüpoteegi summa')"),
  COLLATERAL("The mortgaged property: address and land register number ('kinnistu nr')"),
  EARLY_REPAYMENT_TERMS("Fee or compensation for early repayment ('ennetähtaegne tagastamine')"),
  PROPERTY_VALUE(
      "Value of the mortgaged property used for the loan ('kinnisvara eeldatav väärtus',"
          + " 'turuväärtus' in a valuation)"),
  BALANCE(
      "Outstanding principal with its date: a stated balance ('laenujääk'), or from a repayment"
          + " schedule the balance after the last payment due on or before today");

  private final String description;

  LoanField(String description) {
    this.description = description;
  }

  String description() {
    return description;
  }
}
