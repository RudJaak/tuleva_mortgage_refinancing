package ee.tuleva.mortgage.statement;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/statements")
@RequiredArgsConstructor
class StatementController {

  private final SealedStatementVerifier verifier;

  @PostMapping("/sealed")
  SealedStatementResponse verifySealed(@RequestParam("file") MultipartFile file)
      throws IOException {
    try (var upload = file.getInputStream()) {
      return SealedStatementResponse.from(verifier.verify(upload));
    }
  }

  @ExceptionHandler
  ProblemDetail rejected(SealedStatementException e) {
    var problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    problem.setProperty("reason", e.getReason());
    return problem;
  }

  record SealedStatementResponse(
      String bankName,
      String bankRegistryCode,
      String sealCommonName,
      Instant sealedAt,
      List<FileSummary> files) {

    record FileSummary(String name, String mediaType, int size) {}

    static SealedStatementResponse from(SealedStatement statement) {
      return new SealedStatementResponse(
          statement.bankName(),
          statement.bankRegistryCode(),
          statement.sealCommonName(),
          statement.sealedAt(),
          statement.files().stream()
              .map(f -> new FileSummary(f.name(), f.mediaType(), f.content().length))
              .toList());
    }
  }
}
