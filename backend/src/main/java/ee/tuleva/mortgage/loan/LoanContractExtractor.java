package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.document.DocumentFile;
import java.util.List;

/**
 * Reads a loan contract upload: every PDF in it (the contract, often with its standard information
 * sheet or repayment schedule). Selected by {@code app.extraction.mode} (stub or claude).
 */
interface LoanContractExtractor {

  ExtractedLoanContract extract(List<DocumentFile> pdfs);
}
