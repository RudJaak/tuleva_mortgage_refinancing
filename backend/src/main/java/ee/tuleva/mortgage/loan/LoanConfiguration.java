package ee.tuleva.mortgage.loan;

import ee.tuleva.mortgage.extraction.ClaudeDocumentReader;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LoanConfiguration {

  @Bean
  @ConditionalOnProperty(name = "app.extraction.mode", havingValue = "claude")
  LoanContractExtractor claudeLoanContractExtractor(ClaudeDocumentReader reader, Clock clock) {
    return new ClaudeLoanContractExtractor(reader, clock);
  }

  @Bean
  @ConditionalOnProperty(name = "app.extraction.mode", havingValue = "stub", matchIfMissing = true)
  LoanContractExtractor stubLoanContractExtractor() {
    return new StubLoanContractExtractor();
  }
}
