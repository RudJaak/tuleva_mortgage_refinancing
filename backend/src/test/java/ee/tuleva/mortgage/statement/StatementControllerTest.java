package ee.tuleva.mortgage.statement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatementController.class)
@Import({StatementConfiguration.class, SealedStatementVerifier.class})
class StatementControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void verifiesAnUploadedBankSealedStatement() throws Exception {
    var file =
        new MockMultipartFile(
            "file",
            "statement.asice",
            "application/vnd.etsi.asic-e+zip",
            SealedStatementSample.bytes());

    mockMvc
        .perform(multipart("/v1/statements/sealed").file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bankName").isNotEmpty())
        .andExpect(jsonPath("$.bankRegistryCode").isNotEmpty())
        .andExpect(jsonPath("$.sealedAt").isNotEmpty())
        .andExpect(jsonPath("$.files[0].size").isNumber())
        .andExpect(jsonPath("$.files[0].content").doesNotExist());
  }

  @Test
  void rejectsAnUploadThatIsNotASealedContainer() throws Exception {
    var file =
        new MockMultipartFile(
            "file",
            "statement.pdf",
            "application/pdf",
            "%PDF-1.7".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/v1/statements/sealed").file(file))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.reason").value("NOT_A_CONTAINER"));
  }
}
