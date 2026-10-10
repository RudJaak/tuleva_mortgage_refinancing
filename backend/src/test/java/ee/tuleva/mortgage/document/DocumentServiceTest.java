package ee.tuleva.mortgage.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class DocumentServiceTest {

  private static final byte[] UPLOAD = "%PDF-1.7".getBytes();
  private static final UUID BORROWER = UUID.randomUUID();
  private static final DocumentFile PDF = new DocumentFile("s.pdf", DocumentFile.PDF, UPLOAD);
  private static final Verification SEALED =
      new Verification(
          VerificationLevel.BANK_SEALED,
          "LHV",
          "LHV seal",
          Instant.parse("2026-10-10T06:00:00Z"),
          List.of(),
          null);

  private final ContainerReader reader = mock(ContainerReader.class);
  private final BorrowerService borrowers = mock(BorrowerService.class);
  private final DocumentRepository documents = mock(DocumentRepository.class);
  private final RecordingProcessor processor = new RecordingProcessor();
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC);
  private final List<Document> saved = new ArrayList<>();
  private final List<Runnable> queued = new ArrayList<>();

  private DocumentService service(boolean requireBankSeal, Executor executor) {
    given(documents.save(any()))
        .willAnswer(
            invocation -> {
              saved.add(invocation.getArgument(0));
              return invocation.getArgument(0);
            });
    given(documents.findByBorrowerIdAndTypeOrderByCreatedAtDesc(any(), any()))
        .willAnswer(invocation -> saved.stream().distinct().toList());
    return new DocumentService(
        reader,
        borrowers,
        documents,
        List.of(processor),
        new DocumentProperties(requireBankSeal, List.of()),
        clock,
        executor);
  }

  @Test
  void answersWithTheSealBeforeProcessingFinishes() {
    given(reader.read("s.asice", UPLOAD))
        .willReturn(new ContainerReader.Contents(List.of(PDF), SEALED));
    var service = service(false, queued::add);

    var accepted = service.receive(BORROWER, DocumentType.ACCOUNT_STATEMENT, "s.asice", UPLOAD);

    assertThat(accepted.status()).isEqualTo(DocumentStatus.RECEIVED);
    assertThat(accepted.verification()).isEqualTo(SEALED);
    assertThat(accepted.result()).isNull();
    assertThat(processor.received).isNull();
    assertThat(service.list(BORROWER, DocumentType.ACCOUNT_STATEMENT))
        .singleElement()
        .satisfies(view -> assertThat(view.status()).isEqualTo(DocumentStatus.RECEIVED));

    queued.forEach(Runnable::run);

    assertThat(processor.received.files()).containsExactly(PDF);
    assertThat(processor.received.borrowerId()).isEqualTo(BORROWER);
    assertThat(service.list(BORROWER, DocumentType.ACCOUNT_STATEMENT))
        .singleElement()
        .satisfies(
            view -> {
              assertThat(view.status()).isEqualTo(DocumentStatus.PROCESSED);
              assertThat(view.result()).isEqualTo("processed");
              assertThat(view.verification().bankName()).isEqualTo("LHV");
            });
    assertThat(saved.getLast().getSha256()).hasSize(64);
  }

  @Test
  void aSignedContractRecordsWhetherTheBorrowerSignedIt() {
    var signed =
        new Verification(
            VerificationLevel.SIGNED,
            "LHV",
            null,
            Instant.parse("2026-10-10T06:00:00Z"),
            List.of(new Verification.Signer("Mari Maasikas", Verification.Signer.Kind.PERSON)),
            null);
    given(borrowers.find(BORROWER))
        .willReturn(
            java.util.Optional.of(
                Borrower.identified("60001017869", "Mari", "Maasikas", Instant.now())));
    given(reader.read("c.asice", UPLOAD))
        .willReturn(
            new ContainerReader.Contents(List.of(PDF), signed, java.util.Set.of("60001017869")));

    var service = service(false, queued::add);
    var accepted = service.receive(BORROWER, DocumentType.ACCOUNT_STATEMENT, "c.asice", UPLOAD);

    assertThat(accepted.verification().signedByBorrower()).isTrue();
    assertThat(saved.getLast().getSignedByBorrower()).isTrue();
    assertThat(service.list(BORROWER, DocumentType.ACCOUNT_STATEMENT))
        .as("polling keeps the signers the upload answered with")
        .singleElement()
        .satisfies(
            view -> {
              assertThat(view.verification().signers()).hasSize(1);
              assertThat(view.verification().signedByBorrower()).isTrue();
            });
    assertThat(accepted.verification().signers())
        .extracting(Verification.Signer::name)
        .containsExactly("Mari Maasikas");
  }

  @Test
  void anUnsealedUploadIsRejectedWhenASealIsRequired() {
    given(reader.read("s.pdf", UPLOAD))
        .willReturn(
            new ContainerReader.Contents(
                List.of(PDF), Verification.of(VerificationLevel.PLAIN_PDF)));

    assertThatThrownBy(
            () ->
                service(true, Runnable::run)
                    .receive(BORROWER, DocumentType.ACCOUNT_STATEMENT, "s.pdf", UPLOAD))
        .isInstanceOfSatisfying(
            DocumentRejectedException.class,
            e ->
                assertThat(e.getReason())
                    .isEqualTo(DocumentRejectedException.Reason.SEAL_REQUIRED));
    verify(documents, never()).save(any());
  }

  @Test
  void aTypeWithoutAProcessorIsRejected() {
    assertThatThrownBy(
            () ->
                service(false, Runnable::run)
                    .receive(BORROWER, DocumentType.LOAN_CONTRACT, "c.pdf", UPLOAD))
        .isInstanceOfSatisfying(
            DocumentRejectedException.class,
            e ->
                assertThat(e.getReason())
                    .isEqualTo(DocumentRejectedException.Reason.UNSUPPORTED_TYPE));
  }

  @Test
  void aRejectionDuringProcessingIsReportedWithItsReason() {
    given(reader.read("s.pdf", UPLOAD))
        .willReturn(
            new ContainerReader.Contents(
                List.of(PDF), Verification.of(VerificationLevel.PLAIN_PDF)));
    processor.failWith =
        new DocumentRejectedException(
            DocumentRejectedException.Reason.EXTRACTION_FAILED, "Could not read the statement");
    var service = service(false, Runnable::run);

    service.receive(BORROWER, DocumentType.ACCOUNT_STATEMENT, "s.pdf", UPLOAD);

    assertThat(service.list(BORROWER, DocumentType.ACCOUNT_STATEMENT))
        .singleElement()
        .satisfies(
            view -> {
              assertThat(view.status()).isEqualTo(DocumentStatus.FAILED);
              assertThat(view.failure().reason())
                  .isEqualTo(DocumentRejectedException.Reason.EXTRACTION_FAILED);
            });
  }

  @Test
  void anUnexpectedErrorIsReportedWithoutItsDetails() {
    given(reader.read("s.pdf", UPLOAD))
        .willReturn(
            new ContainerReader.Contents(
                List.of(PDF), Verification.of(VerificationLevel.PLAIN_PDF)));
    processor.failWith = new IllegalStateException("internal detail");
    var service = service(false, Runnable::run);

    service.receive(BORROWER, DocumentType.ACCOUNT_STATEMENT, "s.pdf", UPLOAD);

    assertThat(service.list(BORROWER, DocumentType.ACCOUNT_STATEMENT))
        .singleElement()
        .satisfies(
            view -> {
              assertThat(view.failure().reason())
                  .isEqualTo(DocumentRejectedException.Reason.PROCESSING_FAILED);
              assertThat(view.failure().message()).doesNotContain("internal detail");
            });
  }

  @Test
  void aDocumentLeftUnfinishedByARestartIsReportedAsInterrupted() {
    var orphan =
        Document.received(
            BORROWER, DocumentType.ACCOUNT_STATEMENT, "0".repeat(64), SEALED, clock.instant());
    var service = service(false, Runnable::run);
    saved.add(orphan);

    assertThat(service.list(BORROWER, DocumentType.ACCOUNT_STATEMENT))
        .singleElement()
        .satisfies(
            view -> {
              assertThat(view.status()).isEqualTo(DocumentStatus.FAILED);
              assertThat(view.failure().reason())
                  .isEqualTo(DocumentRejectedException.Reason.INTERRUPTED);
            });
  }

  @Test
  void aDocumentProcessedBeforeARestartShowsItsStoredResult() {
    var earlier =
        Document.received(
            BORROWER, DocumentType.ACCOUNT_STATEMENT, "0".repeat(64), SEALED, clock.instant());
    earlier.processed();
    var service = service(false, Runnable::run);
    saved.add(earlier);

    assertThat(service.list(BORROWER, DocumentType.ACCOUNT_STATEMENT))
        .singleElement()
        .satisfies(
            view -> {
              assertThat(view.status()).isEqualTo(DocumentStatus.PROCESSED);
              assertThat(view.result()).isEqualTo("stored");
            });
  }

  private static class RecordingProcessor implements DocumentProcessor {
    ReceivedDocument received;
    RuntimeException failWith;

    @Override
    public DocumentType type() {
      return DocumentType.ACCOUNT_STATEMENT;
    }

    @Override
    public boolean bankSealCanBeRequired() {
      return true;
    }

    @Override
    public java.util.Optional<Object> storedResult(UUID documentId) {
      return java.util.Optional.of("stored");
    }

    @Override
    public Object process(ReceivedDocument document) {
      received = document;
      if (failWith != null) {
        throw failWith;
      }
      return "processed";
    }
  }
}
