package ee.tuleva.mortgage.extraction;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.extraction")
public record ExtractionProperties(String mode, Claude claude) {

  public record Claude(String apiKey, String model, String effort, long maxTokens) {}
}
