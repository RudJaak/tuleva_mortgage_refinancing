package ee.tuleva.mortgage.document;

import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.NOT_A_CONTAINER;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.UNSUPPORTED_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ee.tuleva.mortgage.document.DocumentProperties.TrustedBank;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class ContainerReaderTest {

  private static final org.digidoc4j.Configuration DIGIDOC =
      new DocumentConfiguration().documentDigiDocConfiguration();
  private static final DocumentProperties BANKS =
      new DocumentProperties(
          false,
          List.of(new TrustedBank("Swedbank", "10060701"), new TrustedBank("LHV", "10539549")));

  private final ContainerReader reader = new ContainerReader(DIGIDOC, BANKS);

  @Test
  void aBankSealedStatementIsVerifiedAndUnwrapped() throws Exception {
    var contents = reader.read("statement.asice", SealedStatementSample.bytes());

    assertThat(contents.verification().level()).isEqualTo(VerificationLevel.BANK_SEALED);
    assertThat(contents.verification().bankName()).isIn("Swedbank", "LHV");
    assertThat(contents.verification().sealedBy()).isNotBlank();
    assertThat(contents.verification().sealedAt()).isNotNull();
    assertThat(contents.files())
        .isNotEmpty()
        .allSatisfy(f -> assertThat(f.content()).isNotEmpty())
        .anyMatch(DocumentFile::isPdf);
  }

  @Test
  void aValidSealFromABankWeDoNotKnowIsUntrusted() throws Exception {
    var noBanks = new ContainerReader(DIGIDOC, new DocumentProperties(false, List.of()));

    var contents = noBanks.read("statement.asice", SealedStatementSample.bytes());

    assertThat(contents.verification().level()).isEqualTo(VerificationLevel.UNTRUSTED_SEAL);
    assertThat(contents.verification().bankName()).isNull();
    assertThat(contents.files()).isNotEmpty();
  }

  @Test
  void aStatementAlteredAfterSealingHasAnInvalidSeal() throws Exception {
    var altered =
        SealedStatementSample.withDataFile(
            SealedStatementSample.bytes(),
            content -> {
              var copy = content.clone();
              copy[copy.length / 2] ^= 1;
              return copy;
            });

    var contents = reader.read("statement.asice", altered);

    assertThat(contents.verification().level()).isEqualTo(VerificationLevel.INVALID_SEAL);
  }

  @Test
  void aFileAddedNextToTheSealedStatementInvalidatesTheSeal() throws Exception {
    var withExtra =
        SealedStatementSample.withExtraFile(
            SealedStatementSample.bytes(),
            "extra.txt",
            "not sealed by the bank".getBytes(StandardCharsets.UTF_8));

    assertThat(reader.read("statement.asice", withExtra).verification().level())
        .isEqualTo(VerificationLevel.INVALID_SEAL);
  }

  @Test
  void aPlainPdfIsAcceptedUnverified() {
    var pdf = "%PDF-1.7 synthetic".getBytes(StandardCharsets.UTF_8);

    var contents = reader.read("statement.pdf", pdf);

    assertThat(contents.verification().level()).isEqualTo(VerificationLevel.PLAIN_PDF);
    assertThat(contents.files()).singleElement().satisfies(f -> assertThat(f.isPdf()).isTrue());
  }

  @Test
  void aZipThatIsNotAContainerIsRejected() throws Exception {
    var zip = new ByteArrayOutputStream();
    try (var out = new ZipOutputStream(zip)) {
      out.putNextEntry(new ZipEntry("notes.txt"));
      out.write("hello".getBytes(StandardCharsets.UTF_8));
      out.closeEntry();
    }

    assertThatThrownBy(() -> reader.read("notes.zip", zip.toByteArray()))
        .isInstanceOfSatisfying(
            DocumentRejectedException.class,
            e -> assertThat(e.getReason()).isEqualTo(NOT_A_CONTAINER));
  }

  @Test
  void anythingElseIsRejected() {
    var image = new byte[] {(byte) 0x89, 'P', 'N', 'G'};

    assertThatThrownBy(() -> reader.read("photo.png", image))
        .isInstanceOfSatisfying(
            DocumentRejectedException.class,
            e -> assertThat(e.getReason()).isEqualTo(UNSUPPORTED_FORMAT));
  }
}
