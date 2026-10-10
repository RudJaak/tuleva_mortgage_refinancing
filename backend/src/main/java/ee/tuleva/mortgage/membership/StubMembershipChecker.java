package ee.tuleva.mortgage.membership;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Answers after a short pause, like a register would; in the demo everyone is a member. */
@Component
@RequiredArgsConstructor
class StubMembershipChecker implements MembershipChecker {

  private final MembershipProperties properties;

  @Override
  public boolean isMember(String personalCode) {
    try {
      Thread.sleep(properties.stubDelay());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return properties.stubMember();
  }
}
