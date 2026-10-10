package ee.tuleva.mortgage.statement;

import ee.tuleva.mortgage.document.DocumentFile;

/** Reads a statement file. Selected by {@code app.extraction.mode} (stub or claude). */
interface StatementExtractor {

  ExtractedStatement extract(DocumentFile statement);
}
