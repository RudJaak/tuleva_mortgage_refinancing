package ee.tuleva.mortgage.statement;

import java.util.Set;

/** The transaction kinds that matter for screening a borrower; everything else is ignored. */
enum TransactionCategory {
  SALARY(Kind.INCOME, "Net wage or salary paid by an employer (e.g. 'töötasu', 'palk')"),
  OTHER_INCOME(
      Kind.INCOME,
      "Other regular income: state benefits, family benefits, pension, rental income. Not"
          + " transfers between the holder's own accounts"),
  MORTGAGE_PAYMENT(Kind.OBLIGATION, "Home loan instalment to a bank ('kodulaen', 'eluasemelaen')"),
  CONSUMER_LOAN(
      Kind.OBLIGATION, "Consumer, car or small loan instalment, including online lenders ('laen')"),
  LEASING(Kind.OBLIGATION, "Leasing payment, usually for a car ('liising')"),
  HIRE_PURCHASE(Kind.OBLIGATION, "Hire purchase or buy-now-pay-later instalment ('järelmaks')"),
  CREDIT_CARD(
      Kind.CARD,
      "Credit card or credit line repayment ('krediitkaart', 'krediidi tagasimakse'), also when"
          + " paid to the holder's own card account"),
  CHILD_SUPPORT(Kind.OBLIGATION, "Child support or alimony paid ('elatis')"),
  LATE_FEE(Kind.WARNING, "Late payment interest or penalty ('viivis', 'leppetrahv')"),
  DEBT_COLLECTION(Kind.WARNING, "Payment to or from a debt collection agency ('inkasso')"),
  ENFORCEMENT(Kind.WARNING, "Bailiff or enforcement seizure ('kohtutäitur', 'arest')"),
  GAMBLING(Kind.WARNING, "Deposit to a gambling or betting operator"),
  SAVINGS(
      Kind.SAVINGS,
      "Money set aside: transfers into savings, investments or a voluntary pension (III pillar),"
          + " including transfers to the holder's own account whose explanation says so ('sääst',"
          + " 'kuu ülejääk', 'hoius', 'investeering'). Not plain transfers between the holder's own"
          + " accounts");

  enum Kind {
    INCOME,
    OBLIGATION,
    /** Repaying card spending: reported as usage, not as a fixed obligation. */
    CARD,
    WARNING,
    SAVINGS
  }

  /** Obligations paid to private persons; the payee's name is never kept. */
  static final Set<TransactionCategory> PRIVATE_PAYEE = Set.of(CHILD_SUPPORT);

  private final Kind kind;
  private final String description;

  TransactionCategory(Kind kind, String description) {
    this.kind = kind;
    this.description = description;
  }

  Kind kind() {
    return kind;
  }

  String description() {
    return description;
  }
}
