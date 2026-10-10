package ee.tuleva.mortgage.borrower;

import java.util.UUID;

/**
 * Asks every module to delete what it holds for a borrower (documents, summaries, the loan), so the
 * flow starts over. Listeners run in the publisher's transaction, ordered so foreign keys hold:
 * loan first, then statements, then documents.
 */
public record BorrowerDataReset(UUID borrowerId) {

  public static final int LOAN_ORDER = 1;
  public static final int STATEMENT_ORDER = 2;
  public static final int DOCUMENT_ORDER = 3;
}
