package ee.tuleva.mortgage.document;

import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.INTERRUPTED;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.NO_USABLE_FILE;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.PROCESSING_FAILED;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.SEAL_REQUIRED;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.UNSUPPORTED_TYPE;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Receives an upload: opens it and checks the seal while the borrower waits, records its metadata,
 * then hands its files to the processor for its type in the background. Extraction can take half a
 * minute, so the borrower polls {@link #list} instead of holding the request open.
 *
 * <p>The upload's bytes live only until processing ends and are never stored. The processor's
 * result (which may hold transactions) is kept in memory only, so it is gone after a restart; the
 * processor persists whatever must last.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class DocumentService {

  record DocumentView(
      UUID documentId,
      DocumentType type,
      DocumentStatus status,
      Verification verification,
      Instant createdAt,
      @Nullable Object result,
      @Nullable Failure failure) {}

  record Failure(DocumentRejectedException.Reason reason, String message) {}

  private record Outcome(@Nullable Object result, @Nullable Failure failure) {}

  private final ContainerReader reader;
  private final BorrowerService borrowers;
  private final DocumentRepository documents;
  private final List<DocumentProcessor> processors;
  private final DocumentProperties properties;
  private final Clock clock;
  private final Executor documentProcessingExecutor;

  private final Map<UUID, Outcome> outcomes = new ConcurrentHashMap<>();
  private final Set<UUID> running = ConcurrentHashMap.newKeySet();

  DocumentView receive(UUID borrowerId, DocumentType type, String fileName, byte[] upload) {
    DocumentProcessor processor =
        processors.stream()
            .filter(p -> p.type() == type)
            .findFirst()
            .orElseThrow(
                () ->
                    new DocumentRejectedException(
                        UNSUPPORTED_TYPE, type + " uploads are not supported yet"));
    var contents = reader.read(fileName, upload);
    var verification = contents.verification();
    if (verification.level() == VerificationLevel.SIGNED) {
      String ownCode = borrowers.find(borrowerId).map(Borrower::getPersonalCode).orElse(null);
      verification =
          verification.withSignedByBorrower(
              ownCode != null && contents.signerCodes().contains(ownCode));
    }
    if (properties.requireBankSeal()
        && processor.bankSealCanBeRequired()
        && verification.level() != VerificationLevel.BANK_SEALED) {
      throw new DocumentRejectedException(SEAL_REQUIRED, "A bank-sealed container is required");
    }
    if (contents.files().isEmpty()) {
      throw new DocumentRejectedException(NO_USABLE_FILE, "The container holds no files");
    }

    var document =
        documents.save(
            Document.received(borrowerId, type, sha256(upload), verification, clock.instant()));
    var received =
        new ReceivedDocument(document.getId(), borrowerId, type, contents.files(), verification);
    running.add(document.getId());
    documentProcessingExecutor.execute(() -> process(document, processor, received));
    return view(document, verification);
  }

  List<DocumentView> list(UUID borrowerId, DocumentType type) {
    return documents.findByBorrowerIdAndTypeOrderByCreatedAtDesc(borrowerId, type).stream()
        .map(
            document ->
                view(
                    document,
                    new Verification(
                        document.getVerificationLevel(),
                        document.getSealedByBank(),
                        null,
                        document.getSealedAt(),
                        List.of(),
                        null)))
        .toList();
  }

  private void process(Document document, DocumentProcessor processor, ReceivedDocument received) {
    try {
      Object result = processor.process(received);
      outcomes.put(document.getId(), new Outcome(result, null));
      document.processed();
    } catch (DocumentRejectedException e) {
      outcomes.put(document.getId(), new Outcome(null, new Failure(e.getReason(), e.getMessage())));
      document.failed();
    } catch (RuntimeException e) {
      log.warn("Processing {} document failed: {}", received.type(), e.getClass().getSimpleName());
      outcomes.put(
          document.getId(),
          new Outcome(null, new Failure(PROCESSING_FAILED, "The document could not be processed")));
      document.failed();
    } finally {
      documents.save(document);
      running.remove(document.getId());
    }
  }

  private DocumentView view(Document document, Verification verification) {
    var outcome = outcomes.get(document.getId());
    DocumentStatus status = document.getStatus();
    Failure failure = outcome != null ? outcome.failure() : null;
    if (status == DocumentStatus.RECEIVED && !running.contains(document.getId())) {
      status = DocumentStatus.FAILED;
      failure = new Failure(INTERRUPTED, "Processing was interrupted, please upload again");
    }
    Object result = outcome != null ? outcome.result() : null;
    if (result == null && status == DocumentStatus.PROCESSED) {
      result =
          processors.stream()
              .filter(p -> p.type() == document.getType())
              .findFirst()
              .flatMap(p -> p.storedResult(document.getId()))
              .orElse(null);
    }
    return new DocumentView(
        document.getId(),
        document.getType(),
        status,
        verification,
        document.getCreatedAt(),
        result,
        failure);
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
