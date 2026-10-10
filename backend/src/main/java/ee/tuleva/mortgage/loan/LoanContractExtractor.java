package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.document.DocumentFile;

/** Reads a loan contract. Selected by {@code app.extraction.mode} (stub or claude). */
interface LoanContractExtractor {

  ExtractedLoanContract extract(DocumentFile contract);
}
