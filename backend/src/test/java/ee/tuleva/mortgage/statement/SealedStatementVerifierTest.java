package ee.tuleva.mortgage.statement;

import static ee.tuleva.mortgage.statement.SealedStatementException.Reason.INVALID_SIGNATURE;
import static ee.tuleva.mortgage.statement.SealedStatementException.Reason.NOT_A_BANK_SEAL;
import static ee.tuleva.mortgage.statement.SealedStatementException.Reason.NOT_A_CONTAINER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ee.tuleva.mortgage.statement.StatementProperties.TrustedBank;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SealedStatementVerifierTest {

  private static final org.digidoc4j.Configuration DIGIDOC =
      new StatementConfiguration().statementDigiDocConfiguration();
  private static final StatementProperties BANKS =
      new StatementProperties(
          List.of(new TrustedBank("Swedbank", "10060701"), new TrustedBank("LHV", "10539549")));

  private final SealedStatementVerifier verifier = new SealedStatementVerifier(DIGIDOC, BANKS);

  @Test
  void acceptsAStatementSealedByAKnownBank() throws Exception {
    var statement = verifier.verify(new ByteArrayInputStream(SealedStatementSample.bytes()));

    assertThat(statement.bankRegistryCode()).isIn("10060701", "10539549");
    assertThat(statement.sealCommonName()).isNotBlank();
    assertThat(statement.sealedAt()).isNotNull();
    assertThat(statement.files())
        .isNotEmpty()
        .allSatisfy(f -> assertThat(f.content()).isNotEmpty());
  }

  @Test
  void rejectsASealFromABankWeDoNotTrust() throws Exception {
    var noBanks = new SealedStatementVerifier(DIGIDOC, new StatementProperties(List.of()));

    assertThatThrownBy(
            () -> noBanks.verify(new ByteArrayInputStream(SealedStatementSample.bytes())))
        .isInstanceOfSatisfying(
            SealedStatementException.class,
            e -> assertThat(e.getReason()).isEqualTo(NOT_A_BANK_SEAL));
  }

  @Test
  void rejectsAStatementAlteredAfterSealing() throws Exception {
    var altered =
        SealedStatementSample.withDataFile(
            SealedStatementSample.bytes(),
            content -> {
              var copy = content.clone();
              copy[copy.length / 2] ^= 1;
              return copy;
            });

    assertThatThrownBy(() -> verifier.verify(new ByteArrayInputStream(altered)))
        .isInstanceOfSatisfying(
            SealedStatementException.class,
            e -> assertThat(e.getReason()).isEqualTo(INVALID_SIGNATURE));
  }

  @Test
  void rejectsAFileAddedNextToTheSealedStatement() throws Exception {
    var withExtra =
        SealedStatementSample.withExtraFile(
            SealedStatementSample.bytes(),
            "extra.txt",
            "not sealed by the bank".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> verifier.verify(new ByteArrayInputStream(withExtra)))
        .isInstanceOfSatisfying(
            SealedStatementException.class,
            e -> assertThat(e.getReason()).isEqualTo(INVALID_SIGNATURE));
  }

  @Test
  void rejectsSomethingThatIsNotAContainer() {
    var pdf = "%PDF-1.7 not a container".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> verifier.verify(new ByteArrayInputStream(pdf)))
        .isInstanceOfSatisfying(
            SealedStatementException.class,
            e -> assertThat(e.getReason()).isEqualTo(NOT_A_CONTAINER));
  }
}
