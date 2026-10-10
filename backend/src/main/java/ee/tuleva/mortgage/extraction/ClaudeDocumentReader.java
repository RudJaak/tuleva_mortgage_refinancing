package ee.tuleva.mortgage.extraction;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends a PDF to Claude and returns its answer as {@code type}, using structured output so the
 * answer always parses. Nothing from the document is logged, only token counts.
 */
@Slf4j
@RequiredArgsConstructor
public class ClaudeDocumentReader {

  // On a refusal, the API retries on another model chosen by the refusal category.
  static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

  private final AnthropicClient client;
  private final ExtractionProperties.Claude settings;

  public <T> T read(byte[] pdf, String systemPrompt, String instruction, Class<T> type) {
    var params =
        MessageCreateParams.builder()
            .model(settings.model())
            .maxTokens(settings.maxTokens())
            .system(systemPrompt)
            .outputConfig(
                StructuredOutputConfig.<T>builder()
                    .format(type)
                    .effort(OutputConfig.Effort.of(settings.effort()))
                    .build())
            .addUserMessageOfBlockParams(
                List.of(
                    ContentBlockParam.ofDocument(
                        DocumentBlockParam.builder()
                            .source(
                                Base64PdfSource.builder()
                                    .data(Base64.getEncoder().encodeToString(pdf))
                                    .build())
                            .build()),
                    ContentBlockParam.ofText(TextBlockParam.builder().text(instruction).build())))
            .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build();
    try {
      var response = client.messages().create(params);
      log.info(
          "Document read as {}: stop={}, inputTokens={}, outputTokens={}",
          type.getSimpleName(),
          response.stopReason().orElse(null),
          response.usage().inputTokens(),
          response.usage().outputTokens());
      if (response.stopReason().filter(StopReason.END_TURN::equals).isEmpty()) {
        throw new ExtractionException(
            "The document could not be read (" + response.stopReason().orElse(null) + ")");
      }
      return response.content().stream()
          .flatMap(block -> block.text().stream())
          .map(text -> text.text())
          .findFirst()
          .orElseThrow(() -> new ExtractionException("Empty extraction result"));
    } catch (AnthropicException e) {
      throw new ExtractionException(
          "The document reader is unavailable: " + e.getClass().getSimpleName(), e);
    }
  }
}
