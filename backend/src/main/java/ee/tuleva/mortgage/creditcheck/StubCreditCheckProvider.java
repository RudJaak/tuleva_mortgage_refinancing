package ee.tuleva.mortgage.creditcheck;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Answers after a pause like a register would, with no defaults; nothing is actually queried. */
@Component
@RequiredArgsConstructor
class StubCreditCheckProvider implements CreditCheckProvider {

  private final CreditCheckProperties properties;

  @Override
  public Report check(String personalCode) {
    try {
      Thread.sleep(properties.stubDelay());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return new Report(0, "Creditinfo maksehäireregister (näidis)");
  }
}
