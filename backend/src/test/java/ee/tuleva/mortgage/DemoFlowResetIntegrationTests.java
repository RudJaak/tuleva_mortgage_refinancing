package ee.tuleva.mortgage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    properties = {
      "app.auth.mode=stub",
      "app.demo.seed-enabled=true",
      "app.demo.fresh-start-on-login=true",
      "app.demo.simulated-joiners.enabled=false",
      "app.extraction.mode=stub",
      "app.documents.require-bank-seal=false"
    })
@AutoConfigureMockMvc
class DemoFlowResetIntegrationTests {

  private static final MockMultipartFile PDF =
      new MockMultipartFile(
          "file", "doc.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.UTF_8));

  @Autowired private MockMvc mockMvc;

  @Test
  void aDemoCanStartOverByResettingOrByLoggingInAgain() throws Exception {
    var session = new MockHttpSession();
    logIn(session);
    fillTheFlow(session);

    mockMvc.perform(post("/v1/demo/flow/reset").session(session)).andExpect(status().isNoContent());
    assertEmpty(session);

    fillTheFlow(session);
    logIn(session);
    assertEmpty(session);
  }

  private void fillTheFlow(MockHttpSession session) throws Exception {
    upload(session, "LOAN_CONTRACT");
    upload(session, "ACCOUNT_STATEMENT");
    mockMvc
        .perform(
            put("/v1/me/loan")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"lender": "SEB", "margin": 1.9, "rateType": "VARIABLE",
                     "maturityDate": "2045-01-01", "repaymentType": "ANNUITY"}
                    """))
        .andExpect(status().isOk());
    mockMvc.perform(get("/v1/me/loan").session(session)).andExpect(status().isOk());
  }

  private void upload(MockHttpSession session, String type) throws Exception {
    mockMvc
        .perform(multipart("/v1/documents").file(PDF).param("type", type).session(session))
        .andExpect(status().isAccepted());
    String status = "RECEIVED";
    for (int i = 0; i < 50 && status.equals("RECEIVED"); i++) {
      Thread.sleep(100);
      status = JsonPath.read(list(session, type), "$[0].status");
    }
    assertThat(status).isEqualTo("PROCESSED");
  }

  private void assertEmpty(MockHttpSession session) throws Exception {
    assertThat(list(session, "LOAN_CONTRACT")).isEqualTo("[]");
    assertThat(list(session, "ACCOUNT_STATEMENT")).isEqualTo("[]");
    mockMvc.perform(get("/v1/me/loan").session(session)).andExpect(status().isNotFound());
  }

  private String list(MockHttpSession session, String type) throws Exception {
    return mockMvc
        .perform(get("/v1/documents").param("type", type).session(session))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private void logIn(MockHttpSession session) throws Exception {
    String loginId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/v1/auth/smart-id")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.loginId");
    String status = "PENDING";
    for (int i = 0; i < 50 && !status.equals("COMPLETE"); i++) {
      status =
          JsonPath.read(
              mockMvc
                  .perform(get("/v1/auth/logins/" + loginId).session(session))
                  .andExpect(jsonPath("$.status").exists())
                  .andReturn()
                  .getResponse()
                  .getContentAsString(),
              "$.status");
      if (!status.equals("COMPLETE")) {
        Thread.sleep(100);
      }
    }
    assertThat(status).isEqualTo("COMPLETE");
  }
}
