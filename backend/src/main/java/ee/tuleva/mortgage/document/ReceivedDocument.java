package ee.tuleva.mortgage.document;

import java.util.List;
import java.util.UUID;

/** An upload that passed intake, handed to the {@link DocumentProcessor} for its type. */
public record ReceivedDocument(
    UUID id,
    UUID borrowerId,
    DocumentType type,
    List<DocumentFile> files,
    Verification verification) {}
