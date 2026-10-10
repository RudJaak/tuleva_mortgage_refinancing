package ee.tuleva.mortgage.statement;

import static ee.tuleva.mortgage.statement.SealedStatementException.Reason.INVALID_SIGNATURE;
import static ee.tuleva.mortgage.statement.SealedStatementException.Reason.NOT_A_BANK_SEAL;
import static ee.tuleva.mortgage.statement.SealedStatementException.Reason.NOT_A_CONTAINER;
import static ee.tuleva.mortgage.statement.SealedStatementException.Reason.UNSIGNED;

import java.io.InputStream;
import java.security.cert.X509Certificate;
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
import org.springframework.stereotype.Service;

/**
 * Verifies a bank-sealed account statement (ASiC-E / BDOC): the container must carry exactly one
 * signature, it must validate against the EE trusted list, and the signing certificate must be an
 * organisation certificate of a configured bank. A single signature means every data file is
 * covered by the bank's seal, so nothing can be added next to the statement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class SealedStatementVerifier {

  private static final Set<String> CONTAINER_TYPES = Set.of("ASICE", "BDOC");
  private static final String ESTONIAN_LEGAL_PERSON_PREFIX = "NTREE-";

  private final org.digidoc4j.Configuration statementDigiDocConfiguration;
  private final StatementProperties properties;

  SealedStatement verify(InputStream upload) {
    Container container = open(upload);
    if (container.getSignatures().size() != 1) {
      throw new SealedStatementException(
          UNSIGNED, "Expected exactly one seal, found " + container.getSignatures().size());
    }
    var validation = container.validate();
    if (!validation.isValid()) {
      log.info("Sealed statement rejected: {} validation errors", validation.getErrors().size());
      throw new SealedStatementException(INVALID_SIGNATURE, "The seal is not valid");
    }

    Signature seal = container.getSignatures().getFirst();
    X509Certificate certificate = seal.getSigningCertificate().getX509Certificate();
    X500Name subject = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
    String registryCode =
        attribute(subject, BCStyle.ORGANIZATION_IDENTIFIER)
            .filter(id -> id.startsWith(ESTONIAN_LEGAL_PERSON_PREFIX))
            .map(id -> id.substring(ESTONIAN_LEGAL_PERSON_PREFIX.length()))
            .orElseThrow(
                () ->
                    new SealedStatementException(
                        NOT_A_BANK_SEAL, "The seal is not an Estonian organisation's"));
    var bank =
        properties
            .bankWithRegistryCode(registryCode)
            .orElseThrow(
                () ->
                    new SealedStatementException(
                        NOT_A_BANK_SEAL, "Sealed by " + registryCode + ", not a known bank"));

    log.info("Sealed statement verified: bank={}", bank.name());
    return new SealedStatement(
        bank.name(),
        bank.registryCode(),
        attribute(subject, BCStyle.CN).orElse(bank.name()),
        seal.getTrustedSigningTime().toInstant(),
        container.getDataFiles().stream()
            .map(
                file ->
                    new SealedStatement.File(file.getName(), file.getMediaType(), file.getBytes()))
            .toList());
  }

  private Container open(InputStream upload) {
    Container container;
    try {
      container =
          ContainerBuilder.aContainer()
              .withConfiguration(statementDigiDocConfiguration)
              .fromStream(upload)
              .build();
    } catch (DigiDoc4JException e) {
      throw new SealedStatementException(NOT_A_CONTAINER, "Not a readable container", e);
    }
    if (!CONTAINER_TYPES.contains(container.getType())) {
      throw new SealedStatementException(
          NOT_A_CONTAINER, "Unsupported container type " + container.getType());
    }
    return container;
  }

  private static Optional<String> attribute(X500Name name, ASN1ObjectIdentifier type) {
    RDN[] rdns = name.getRDNs(type);
    return rdns.length == 0
        ? Optional.empty()
        : Optional.of(IETFUtils.valueToString(rdns[0].getFirst().getValue()));
  }
}
