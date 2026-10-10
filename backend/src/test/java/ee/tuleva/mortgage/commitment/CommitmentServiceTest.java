package ee.tuleva.mortgage.commitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ee.tuleva.mortgage.auth.AuthMethod;
import ee.tuleva.mortgage.auth.AuthProvider;
import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import ee.tuleva.mortgage.loan.ConfirmedLoans;
import ee.tuleva.mortgage.pool.Commitment;
import ee.tuleva.mortgage.pool.CommitmentRepository;
import ee.tuleva.mortgage.pool.CommitmentStatus;
import ee.tuleva.mortgage.pool.PoolProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommitmentServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");
  private static final Borrower MARI = Borrower.identified("60001017869", "Mari", "Tamm", NOW);
  private static final UUID ID = MARI.getId();

  private final AuthProvider auth = mock(AuthProvider.class);
  private final BorrowerService borrowers = mock(BorrowerService.class);
  private final ConfirmedLoans loans = mock(ConfirmedLoans.class);
  private final CommitmentRepository commitments = mock(CommitmentRepository.class);
  private final CommitmentService service =
      new CommitmentService(
          auth,
          borrowers,
          loans,
          commitments,
          new PoolProperties(
              OffsetDateTime.parse("2026-12-31T23:59:59+02:00"),
              new BigDecimal("100000000"),
              List.of(
                  new PoolProperties.Milestone(new BigDecimal("100000000"), new BigDecimal("1.20")),
                  new PoolProperties.Milestone(
                      new BigDecimal("500000000"), new BigDecimal("0.80"))),
              10),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void signingCreatesTheCommitmentWithTheConfirmedFigures() {
    loan("150000", "1.85");
    given(borrowers.find(ID)).willReturn(Optional.of(MARI));
    given(auth.start(AuthMethod.SMART_ID, "60001017869", null))
        .willReturn(new AuthProvider.Challenge("s1", "1234", "Mari"));
    given(auth.poll("s1"))
        .willReturn(new AuthProvider.Outcome(AuthProvider.Status.PENDING, null))
        .willReturn(
            new AuthProvider.Outcome(
                AuthProvider.Status.COMPLETE,
                new AuthProvider.Person("60001017869", "Mari", "Tamm")));

    var start = service.startSigning(ID, AuthMethod.SMART_ID, null);
    assertThat(start.verificationCode()).isEqualTo("1234");
    assertThat(service.poll(ID, "s1")).isEqualTo(AuthProvider.Status.PENDING);
    assertThat(service.poll(ID, "s1")).isEqualTo(AuthProvider.Status.COMPLETE);

    var saved = ArgumentCaptor.forClass(Commitment.class);
    verify(commitments).save(saved.capture());
    assertThat(saved.getValue().getCommittedBalance()).isEqualByComparingTo("150000");
    assertThat(saved.getValue().getCurrentMargin()).isEqualByComparingTo("1.85");
    assertThat(saved.getValue().getStatus()).isEqualTo(CommitmentStatus.SIGNED);
  }

  @Test
  void needsAConfirmedLoanWithBalanceAndMargin() {
    given(loans.of(ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.startSigning(ID, AuthMethod.SMART_ID, null))
        .isInstanceOfSatisfying(
            CommitmentRejectedException.class,
            e ->
                assertThat(e.getReason())
                    .isEqualTo(CommitmentRejectedException.Reason.LOAN_NOT_CONFIRMED));
    verify(auth, never()).start(any(), any(), any());
  }

  @Test
  void aMarginTheBestTargetCannotBeatIsNotCommitted() {
    loan("150000", "0.80");

    assertThatThrownBy(() -> service.startSigning(ID, AuthMethod.SMART_ID, null))
        .isInstanceOfSatisfying(
            CommitmentRejectedException.class,
            e ->
                assertThat(e.getReason())
                    .isEqualTo(CommitmentRejectedException.Reason.NO_POSSIBLE_GAIN));
  }

  @Test
  void commitsOnlyOnce() {
    given(commitments.findFirstByBorrowerIdAndStatus(ID, CommitmentStatus.SIGNED))
        .willReturn(Optional.of(Commitment.signed(ID, BigDecimal.TEN, BigDecimal.ONE, NOW)));

    assertThatThrownBy(() -> service.startSigning(ID, AuthMethod.SMART_ID, null))
        .isInstanceOfSatisfying(
            CommitmentRejectedException.class,
            e ->
                assertThat(e.getReason())
                    .isEqualTo(CommitmentRejectedException.Reason.ALREADY_COMMITTED));
    assertThat(service.current(ID).committed()).isTrue();
  }

  @Test
  void someoneElseCannotCompleteTheSignature() {
    loan("150000", "1.85");
    given(borrowers.find(ID)).willReturn(Optional.of(MARI));
    given(auth.start(any(), any(), any()))
        .willReturn(new AuthProvider.Challenge("s2", "1234", null));
    service.startSigning(ID, AuthMethod.SMART_ID, null);

    assertThat(service.poll(UUID.randomUUID(), "s2")).isEqualTo(AuthProvider.Status.FAILED);
    verify(auth, never()).poll(any());
  }

  private void loan(String balance, String margin) {
    given(loans.of(ID))
        .willReturn(
            Optional.of(
                new ConfirmedLoans.Figures(
                    "SEB", new BigDecimal(balance), new BigDecimal(margin), 240)));
  }
}
