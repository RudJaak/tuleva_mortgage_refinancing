package ee.tuleva.mortgage.commitment;

import static ee.tuleva.mortgage.commitment.CommitmentRejectedException.Reason.ALREADY_COMMITTED;
import static ee.tuleva.mortgage.commitment.CommitmentRejectedException.Reason.LOAN_NOT_CONFIRMED;
import static ee.tuleva.mortgage.commitment.CommitmentRejectedException.Reason.NO_POSSIBLE_GAIN;

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
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The conditional commitment (PLAN.md "Commitment model"): the borrower confirms with an emulated
 * Smart-ID or Mobile-ID signature (D29, T41: signing by re-authentication) that they will switch if
 * the pool's target margin at close is below their own. Signing creates the commitment that the
 * pool tracker counts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class CommitmentService {

  record CommitmentView(
      boolean committed,
      @Nullable BigDecimal committedBalance,
      @Nullable BigDecimal currentMargin,
      @Nullable Instant signedAt) {}

  record SigningStart(String signingId, String verificationCode, @Nullable String firstName) {}

  private record PendingSigning(
      UUID borrowerId, String personalCode, BigDecimal balance, BigDecimal margin) {}

  private final AuthProvider authProvider;
  private final BorrowerService borrowers;
  private final ConfirmedLoans confirmedLoans;
  private final CommitmentRepository commitments;
  private final PoolProperties pool;
  private final Clock clock;
  private final Map<String, PendingSigning> signings = new ConcurrentHashMap<>();

  CommitmentView current(UUID borrowerId) {
    return signed(borrowerId)
        .map(
            c ->
                new CommitmentView(
                    true, c.getCommittedBalance(), c.getCurrentMargin(), c.getSignedAt()))
        .orElse(new CommitmentView(false, null, null, null));
  }

  SigningStart startSigning(UUID borrowerId, AuthMethod method, @Nullable String phoneNumber) {
    if (signed(borrowerId).isPresent()) {
      throw new CommitmentRejectedException(ALREADY_COMMITTED, "Already committed");
    }
    var loan =
        confirmedLoans
            .of(borrowerId)
            .filter(l -> l.balance() != null && l.balance().signum() > 0 && l.margin() != null)
            .orElseThrow(
                () ->
                    new CommitmentRejectedException(
                        LOAN_NOT_CONFIRMED, "Confirm the loan's balance and margin first"));
    if (loan.margin().compareTo(bestTargetMargin()) <= 0) {
      throw new CommitmentRejectedException(
          NO_POSSIBLE_GAIN, "The margin is already at or below the pool's best target");
    }
    String personalCode =
        borrowers
            .find(borrowerId)
            .map(Borrower::getPersonalCode)
            .orElseThrow(() -> new IllegalStateException("Logged-in borrower without a code"));

    var challenge = authProvider.start(method, personalCode, phoneNumber);
    signings.put(
        challenge.loginId(),
        new PendingSigning(borrowerId, personalCode, loan.balance(), loan.margin()));
    return new SigningStart(
        challenge.loginId(), challenge.verificationCode(), challenge.firstName());
  }

  /** Polls the signature; on completion the commitment is created (once). */
  @Transactional
  AuthProvider.Status poll(UUID borrowerId, String signingId) {
    PendingSigning pending = signings.get(signingId);
    if (pending == null || !pending.borrowerId().equals(borrowerId)) {
      return AuthProvider.Status.FAILED;
    }
    var outcome = authProvider.poll(signingId);
    if (outcome.status() != AuthProvider.Status.COMPLETE) {
      if (outcome.status() == AuthProvider.Status.FAILED) {
        signings.remove(signingId);
      }
      return outcome.status();
    }
    signings.remove(signingId);
    if (outcome.person() == null
        || !pending.personalCode().equals(outcome.person().personalCode())) {
      return AuthProvider.Status.FAILED;
    }
    if (signed(borrowerId).isEmpty()) {
      commitments.save(
          Commitment.signed(borrowerId, pending.balance(), pending.margin(), clock.instant()));
      log.info("Commitment signed");
    }
    return AuthProvider.Status.COMPLETE;
  }

  void cancel(UUID borrowerId, String signingId) {
    PendingSigning pending = signings.get(signingId);
    if (pending != null && pending.borrowerId().equals(borrowerId)) {
      signings.remove(signingId);
      authProvider.cancel(signingId);
    }
  }

  /** The lowest margin the pool aims for; a loan already at or below it cannot gain. */
  BigDecimal bestTargetMargin() {
    return pool.milestones().stream()
        .map(PoolProperties.Milestone::margin)
        .min(Comparator.naturalOrder())
        .orElse(BigDecimal.ZERO);
  }

  private Optional<Commitment> signed(UUID borrowerId) {
    return commitments.findFirstByBorrowerIdAndStatus(borrowerId, CommitmentStatus.SIGNED);
  }
}
