package ee.tuleva.mortgage.document;

import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.NOT_A_CONTAINER;
import static ee.tuleva.mortgage.document.DocumentRejectedException.Reason.UNSUPPORTED_FORMAT;
import static ee.tuleva.mortgage.document.VerificationLevel.BANK_SEALED;
import static ee.tuleva.mortgage.document.VerificationLevel.INVALID_SEAL;
import static ee.tuleva.mortgage.document.VerificationLevel.PLAIN_PDF;
import static ee.tuleva.mortgage.document.VerificationLevel.SIGNED;
import static ee.tuleva.mortgage.document.VerificationLevel.UNSIGNED_CONTAINER;
import static ee.tuleva.mortgage.document.VerificationLevel.UNTRUSTED_SEAL;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.Signature;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Opens an upload and reports what it contains and how far it can be trusted. A bank-sealed ASiC-E
 * / BDOC container must carry exactly one signature that validates against the EE trusted list,
 * from an organisation certificate of a configured bank. A single signature means every data file
 * is covered by the bank's seal, so nothing can be added next to the statement. Seal problems lower
 * the verification level instead of failing; only unreadable input is rejected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ContainerReader {

  /** {@code signerCodes} are the personal codes of signing people, for the borrower check. */
  record Contents(List<DocumentFile> files, Verification verification, Set<String> signerCodes) {
    Contents(List<DocumentFile> files, Verification verification) {
      this(files, verification, Set.of());
    }
  }

  private record SignerIdentity(Verification.Signer signer, @Nullable String code) {}

  private static final Set<String> CONTAINER_TYPES = Set.of("ASICE", "BDOC");
  private static final String ESTONIAN_LEGAL_PERSON_PREFIX = "NTREE-";
  private static final String ESTONIAN_PERSON_PREFIX = "PNOEE-";

  private final org.digidoc4j.Configuration documentDigiDocConfiguration;
  private final DocumentProperties properties;

  Contents read(String fileName, byte[] upload) {
    if (startsWith(upload, "%PDF")) {
      return new Contents(
          List.of(new DocumentFile(fileName, DocumentFile.PDF, upload)),
          Verification.of(PLAIN_PDF));
    }
    if (!startsWith(upload, "PK")) {
      throw new DocumentRejectedException(
          UNSUPPORTED_FORMAT, "Expected a PDF or a signed container");
    }
    Container container = open(upload);
    List<DocumentFile> files =
        container.getDataFiles().stream()
            .map(file -> new DocumentFile(file.getName(), file.getMediaType(), file.getBytes()))
            .toList();
    return verify(container, files);
  }

  private Contents verify(Container container, List<DocumentFile> files) {
    List<Signature> signatures = container.getSignatures();
    if (signatures.isEmpty()) {
      return new Contents(files, Verification.of(UNSIGNED_CONTAINER));
    }
    var validation = container.validate();
    if (!validation.isValid()) {
      log.info("Container signatures invalid: {} validation errors", validation.getErrors().size());
      return new Contents(files, Verification.of(INVALID_SEAL));
    }

    List<SignerIdentity> identities = signatures.stream().map(this::identify).toList();
    var signers = identities.stream().map(SignerIdentity::signer).toList();
    var signerCodes =
        identities.stream()
            .map(SignerIdentity::code)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    Instant signedAt = signatures.getLast().getTrustedSigningTime().toInstant();
    Optional<DocumentProperties.TrustedBank> bank =
        signatures.stream()
            .map(ContainerReader::bankRegistryCode)
            .flatMap(Optional::stream)
            .map(properties::bankWithRegistryCode)
            .flatMap(Optional::stream)
            .findFirst();

    // A single seal covers every file, so only then is the bank vouching for all of them
    if (signatures.size() == 1
        && signers.getFirst().kind() == Verification.Signer.Kind.ORGANISATION) {
      var level = bank.isPresent() ? BANK_SEALED : UNTRUSTED_SEAL;
      bank.ifPresent(b -> log.info("Container sealed by bank={}", b.name()));
      return new Contents(
          files,
          new Verification(
              level,
              bank.map(DocumentProperties.TrustedBank::name).orElse(null),
              signers.getFirst().name(),
              signedAt,
              signers,
              null),
          signerCodes);
    }
    log.info("Container signed by {} signatures", signatures.size());
    return new Contents(
        files,
        new Verification(
            SIGNED,
            bank.map(DocumentProperties.TrustedBank::name).orElse(null),
            null,
            signedAt,
            signers,
            null),
        signerCodes);
  }

  private SignerIdentity identify(Signature signature) {
    X500Name subject = subject(signature);
    var organisationId = attribute(subject, BCStyle.ORGANIZATION_IDENTIFIER);
    if (organisationId.isPresent()) {
      String name =
          attribute(subject, BCStyle.O).or(() -> attribute(subject, BCStyle.CN)).orElse("");
      return new SignerIdentity(
          new Verification.Signer(name, Verification.Signer.Kind.ORGANISATION), null);
    }
    String name =
        (attribute(subject, BCStyle.GIVENNAME).orElse("")
                + " "
                + attribute(subject, BCStyle.SURNAME).orElse(""))
            .strip();
    if (name.isEmpty()) {
      name = attribute(subject, BCStyle.CN).orElse("");
    }
    String code =
        attribute(subject, BCStyle.SERIALNUMBER)
            .map(
                serial ->
                    serial.startsWith(ESTONIAN_PERSON_PREFIX)
                        ? serial.substring(ESTONIAN_PERSON_PREFIX.length())
                        : serial)
            .orElse(null);
    return new SignerIdentity(new Verification.Signer(name, Verification.Signer.Kind.PERSON), code);
  }

  private static Optional<String> bankRegistryCode(Signature signature) {
    return attribute(subject(signature), BCStyle.ORGANIZATION_IDENTIFIER)
        .filter(id -> id.startsWith(ESTONIAN_LEGAL_PERSON_PREFIX))
        .map(id -> id.substring(ESTONIAN_LEGAL_PERSON_PREFIX.length()));
  }

  private static X500Name subject(Signature signature) {
    X509Certificate certificate = signature.getSigningCertificate().getX509Certificate();
    return X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
  }

  private Container open(byte[] upload) {
    Container container;
    try {
      container =
          ContainerBuilder.aContainer()
              .withConfiguration(documentDigiDocConfiguration)
              .fromStream(new ByteArrayInputStream(upload))
              .build();
    } catch (DigiDoc4JException e) {
      throw new DocumentRejectedException(NOT_A_CONTAINER, "Not a readable container", e);
    }
    if (!CONTAINER_TYPES.contains(container.getType())) {
      throw new DocumentRejectedException(
          NOT_A_CONTAINER, "Unsupported container type " + container.getType());
    }
    return container;
  }

  private static boolean startsWith(byte[] content, String magic) {
    if (content.length < magic.length()) {
      return false;
    }
    for (int i = 0; i < magic.length(); i++) {
      if (content[i] != magic.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private static Optional<String> attribute(X500Name name, ASN1ObjectIdentifier type) {
    RDN[] rdns = name.getRDNs(type);
    return rdns.length == 0
        ? Optional.empty()
        : Optional.of(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
  }
}
