package ee.tuleva.mortgage.extraction;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExtractionProperties.class)
class ExtractionConfiguration {

  @Bean
  @ConditionalOnProperty(name = "app.extraction.mode", havingValue = "claude")
  ClaudeDocumentReader claudeDocumentReader(ExtractionProperties properties) {
    var claude = properties.claude();
    if (claude.apiKey() == null || claude.apiKey().isBlank()) {
      throw new IllegalStateException("EXTRACTION_MODE=claude needs ANTHROPIC_API_KEY");
    }
    var client = AnthropicOkHttpClient.builder().apiKey(claude.apiKey()).build();
    return new ClaudeDocumentReader(client, claude);
  }
}
