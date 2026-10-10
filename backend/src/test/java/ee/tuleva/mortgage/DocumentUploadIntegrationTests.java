package ee.tuleva.mortgage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import ee.tuleva.mortgage.borrower.BorrowerService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    properties = {
      "app.demo.seed-enabled=false",
      "app.demo.simulated-joiners.enabled=false",
      "app.extraction.mode=stub",
      "app.documents.require-bank-seal=false"
    })
@AutoConfigureMockMvc
class DocumentUploadIntegrationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private BorrowerService borrowerService;
  @Autowired private JdbcClient jdbc;

  @Test
  void aStatementUploadIsSummarisedAndOnlyTheSummaryIsKept() throws Exception {
    var borrower = borrowerService.findOrCreate("60001017869", "Mary Ann", "O'Connež");
    var session = new MockHttpSession();
    session.setAttribute("borrowerId", borrower.getId());
    var pdf =
        new MockMultipartFile(
            "file",
            "statement.pdf",
            "application/pdf",
            "%PDF-1.7 synthetic".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/v1/documents")
                .file(pdf)
                .param("type", "ACCOUNT_STATEMENT")
                .session(session))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.verification.level").value("PLAIN_PDF"));

    String status = "RECEIVED";
    for (int i = 0; i < 50 && status.equals("RECEIVED"); i++) {
      Thread.sleep(100);
      status =
          JsonPath.read(
              mockMvc
                  .perform(get("/v1/documents").param("type", "ACCOUNT_STATEMENT").session(session))
                  .andReturn()
                  .getResponse()
                  .getContentAsString(),
              "$[0].status");
    }
    mockMvc
        .perform(get("/v1/documents").param("type", "ACCOUNT_STATEMENT").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PROCESSED"))
        .andExpect(jsonPath("$[0].result.summary.income.averageMonthlyTotal").value(2480.00))
        .andExpect(jsonPath("$[0].result.summary.monthlyObligations").value(1001.40))
        .andExpect(jsonPath("$[0].result.summary.monthlyCardRepayments").value(1200.00))
        .andExpect(jsonPath("$[0].result.summary.oneOffPayments.length()").value(1))
        .andExpect(jsonPath("$[0].result.summary.flags[?(@.code == 'LATE_FEES')]").exists())
        .andExpect(jsonPath("$[0].result.counted.length()").value(38));

    var stored =
        jdbc.sql(
                """
                select d.status, d.verification_level, s.average_monthly_income,
                       s.summary ->> 'holderCheck' as holder_check
                from document d join statement_summary s on s.document_id = d.id
                where d.borrower_id = ?
                """)
            .param(borrower.getId())
            .query(
                (rs, row) ->
                    rs.getString("status")
                        + "/"
                        + rs.getString("verification_level")
                        + "/"
                        + rs.getBigDecimal("average_monthly_income").toPlainString()
                        + "/"
                        + rs.getString("holder_check"))
            .single();
    assertThat(stored).isEqualTo("PROCESSED/PLAIN_PDF/2480.00/NOT_CHECKED");
  }

  @Test
  void aLoanContractIsReadAndItsFiguresKept() throws Exception {
    var borrower = borrowerService.findOrCreate("39901012239", "Jaan", "Tamm");
    var session = new MockHttpSession();
    session.setAttribute("borrowerId", borrower.getId());
    var pdf =
        new MockMultipartFile(
            "file",
            "contract.pdf",
            "application/pdf",
            "%PDF-1.7 synthetic".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/v1/documents").file(pdf).param("type", "LOAN_CONTRACT").session(session))
        .andExpect(status().isAccepted());

    String status = "RECEIVED";
    for (int i = 0; i < 50 && status.equals("RECEIVED"); i++) {
      Thread.sleep(100);
      status =
          JsonPath.read(
              mockMvc
                  .perform(get("/v1/documents").param("type", "LOAN_CONTRACT").session(session))
                  .andReturn()
                  .getResponse()
                  .getContentAsString(),
              "$[0].status");
    }
    mockMvc
        .perform(get("/v1/documents").param("type", "LOAN_CONTRACT").session(session))
        .andExpect(jsonPath("$[0].status").value("PROCESSED"))
        .andExpect(jsonPath("$[0].result.contract.lender").value("Swedbank AS"))
        .andExpect(jsonPath("$[0].result.contract.margin").value(1.95))
        .andExpect(jsonPath("$[0].result.contract.evidence.MARGIN.page").value(2))
        .andExpect(jsonPath("$[0].result.summary.balanceSource").value("ESTIMATED"));

    var stored =
        jdbc.sql(
                "select lender, margin, balance_estimated from loan_contract where borrower_id = ?")
            .param(borrower.getId())
            .query(
                (rs, row) -> rs.getString(1) + "/" + rs.getBigDecimal(2) + "/" + rs.getBoolean(3))
            .single();
    assertThat(stored).isEqualTo("Swedbank AS/1.950/true");
  }
}
