package ee.tuleva.mortgage.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

  private static final MockMultipartFile FILE =
      new MockMultipartFile("file", "statement.pdf", "application/pdf", "%PDF-1.7".getBytes());

  @Autowired private MockMvc mockMvc;
  @MockitoBean private DocumentService documentService;

  @Test
  void requiresALoggedInBorrower() throws Exception {
    mockMvc
        .perform(multipart("/v1/documents").file(FILE).param("type", "ACCOUNT_STATEMENT"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void acceptsTheUploadAndReturnsTheSealStraightAway() throws Exception {
    var borrowerId = UUID.randomUUID();
    var documentId = UUID.randomUUID();
    given(
            documentService.receive(
                eq(borrowerId), eq(DocumentType.ACCOUNT_STATEMENT), eq("statement.pdf"), any()))
        .willReturn(view(documentId, DocumentStatus.RECEIVED, null));

    mockMvc
        .perform(
            multipart("/v1/documents")
                .file(FILE)
                .param("type", "ACCOUNT_STATEMENT")
                .session(session(borrowerId)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.documentId").value(documentId.toString()))
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.verification.level").value("PLAIN_PDF"));
  }

  @Test
  void listsTheBorrowersDocumentsWithTheirResults() throws Exception {
    var borrowerId = UUID.randomUUID();
    var documentId = UUID.randomUUID();
    given(documentService.list(borrowerId, DocumentType.ACCOUNT_STATEMENT))
        .willReturn(List.of(view(documentId, DocumentStatus.PROCESSED, Map.of("months", 6))));

    mockMvc
        .perform(
            get("/v1/documents").param("type", "ACCOUNT_STATEMENT").session(session(borrowerId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PROCESSED"))
        .andExpect(jsonPath("$[0].result.months").value(6));
  }

  @Test
  void listingRequiresALoggedInBorrower() throws Exception {
    mockMvc
        .perform(get("/v1/documents").param("type", "ACCOUNT_STATEMENT"))
        .andExpect(status().isUnauthorized());
  }

  private static DocumentService.DocumentView view(
      UUID documentId, DocumentStatus status, Object result) {
    return new DocumentService.DocumentView(
        documentId,
        DocumentType.ACCOUNT_STATEMENT,
        status,
        Verification.of(VerificationLevel.PLAIN_PDF),
        Instant.parse("2026-10-10T10:00:00Z"),
        result,
        null);
  }

  @Test
  void aRejectedUploadIsUnprocessableWithAReason() throws Exception {
    var borrowerId = UUID.randomUUID();
    given(documentService.receive(any(), any(), any(), any()))
        .willThrow(
            new DocumentRejectedException(
                DocumentRejectedException.Reason.UNSUPPORTED_FORMAT, "Expected a PDF"));

    mockMvc
        .perform(
            multipart("/v1/documents")
                .file(FILE)
                .param("type", "ACCOUNT_STATEMENT")
                .session(session(borrowerId)))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.reason").value("UNSUPPORTED_FORMAT"));
  }

  private static MockHttpSession session(UUID borrowerId) {
    var session = new MockHttpSession();
    session.setAttribute("borrowerId", borrowerId);
    return session;
  }
}
