package ee.tuleva.mortgage.document;

import ee.tuleva.mortgage.auth.CurrentBorrower;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/documents")
@RequiredArgsConstructor
class DocumentController {

  private final DocumentService documentService;

  /** Answers once the upload is opened and its seal checked; extraction continues afterwards. */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  DocumentService.DocumentView upload(
      @RequestParam DocumentType type, @RequestParam MultipartFile file, HttpSession session)
      throws IOException {
    String fileName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
    return documentService.receive(borrowerId(session), type, fileName, file.getBytes());
  }

  /** The borrower's documents of one type, newest first; poll while any is still RECEIVED. */
  @GetMapping
  List<DocumentService.DocumentView> list(@RequestParam DocumentType type, HttpSession session) {
    return documentService.list(borrowerId(session), type);
  }

  private static UUID borrowerId(HttpSession session) {
    return CurrentBorrower.id(session)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  @ExceptionHandler
  ProblemDetail rejected(DocumentRejectedException e) {
    var problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    problem.setProperty("reason", e.getReason());
    return problem;
  }
}
