package ee.tuleva.mortgage.demo;

import ee.tuleva.mortgage.auth.BorrowerLoggedIn;
import ee.tuleva.mortgage.borrower.BorrowerDataReset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets a demo start over: the stub login always signs in the same person, so without this every run
 * would land back in the previous run's documents and loan. Real clients keep their data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DemoFlowReset {

  private final ApplicationEventPublisher events;
  private final DemoProperties properties;

  @Transactional
  void reset(UUID borrowerId) {
    events.publishEvent(new BorrowerDataReset(borrowerId));
    log.info("Demo flow reset for a borrower");
  }

  @EventListener
  @Transactional
  void on(BorrowerLoggedIn login) {
    if (properties.freshStartOnLogin()) {
      reset(login.borrowerId());
    }
  }
}
