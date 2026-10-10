package ee.tuleva.mortgage.statement;

import ee.tuleva.mortgage.extraction.ClaudeDocumentReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StatementProperties.class)
class StatementConfiguration {

  @Bean
  @ConditionalOnProperty(name = "app.extraction.mode", havingValue = "claude")
  StatementExtractor claudeStatementExtractor(ClaudeDocumentReader reader) {
    return new ClaudeStatementExtractor(reader);
  }

  @Bean
  @ConditionalOnProperty(name = "app.extraction.mode", havingValue = "stub", matchIfMissing = true)
  StatementExtractor stubStatementExtractor() {
    return new StubStatementExtractor();
  }
}
