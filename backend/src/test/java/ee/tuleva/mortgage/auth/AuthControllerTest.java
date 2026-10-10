package ee.tuleva.mortgage.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ee.tuleva.mortgage.borrower.Borrower;
import ee.tuleva.mortgage.borrower.BorrowerService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

  private static final Borrower MARI =
      Borrower.identified("60001017869", "Mari", "Maasikas", Instant.parse("2026-10-10T10:00:00Z"));

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthProvider authProvider;
  @MockitoBean private BorrowerService borrowerService;

  @Test
  void startsAMobileIdLoginAndShowsTheVerificationCode() throws Exception {
    given(authProvider.start(AuthMethod.MOBILE_ID, "60001017869", "+37268000769"))
        .willReturn(new AuthProvider.Challenge("login-1", "1234", "Mari"));

    mockMvc
        .perform(
            post("/v1/auth/mobile-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"personalCode":"60001017869","phoneNumber":"+37268000769"}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginId").value("login-1"))
        .andExpect(jsonPath("$.verificationCode").value("1234"))
        .andExpect(jsonPath("$.firstName").value("Mari"));
  }

  @Test
  void smartIdNeedsAPersonalCodeButNoPhone() throws Exception {
    mockMvc
        .perform(post("/v1/auth/smart-id").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/v1/auth/id-card")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"personalCode":"60001017869"}"""))
        .andExpect(status().isNotFound());

    verify(authProvider, never()).start(any(), any(), any());
  }

  @Test
  void rejectsAnInvalidPersonalCodeBeforeContactingTheProvider() throws Exception {
    mockMvc
        .perform(
            post("/v1/auth/mobile-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"personalCode":"60001017860","phoneNumber":"+37268000769"}"""))
        .andExpect(status().isBadRequest());

    verify(authProvider, never()).start(any(), any(), any());
  }

  @Test
  void completesTheLoginIntoTheSessionAndThenKnowsWhoYouAre() throws Exception {
    var session = new MockHttpSession();
    given(authProvider.start(eq(AuthMethod.SMART_ID), any(), any()))
        .willReturn(new AuthProvider.Challenge("login-2", "5678", "Mari"));
    given(authProvider.poll("login-2"))
        .willReturn(
            AuthProvider.Outcome.complete(
                new AuthProvider.Person("60001017869", "Mari", "Maasikas")));
    given(borrowerService.findOrCreate("60001017869", "Mari", "Maasikas")).willReturn(MARI);
    given(borrowerService.find(MARI.getId())).willReturn(Optional.of(MARI));

    mockMvc
        .perform(
            post("/v1/auth/smart-id")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"personalCode":"60001017869"}"""))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/v1/auth/logins/login-2").session(session))
        .andExpect(jsonPath("$.status").value("COMPLETE"))
        .andExpect(jsonPath("$.firstName").value("Mari"));
    mockMvc
        .perform(get("/v1/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Mari"))
        .andExpect(jsonPath("$.loginMethod").value("SMART_ID"))
        .andExpect(jsonPath("$.personalCode").doesNotExist());
  }

  @Test
  void anotherBrowserCannotPollSomeoneElsesLogin() throws Exception {
    mockMvc
        .perform(get("/v1/auth/logins/login-3").session(new MockHttpSession()))
        .andExpect(status().isNotFound());

    verify(authProvider, never()).poll(any());
  }

  @Test
  void cancelsAPendingLogin() throws Exception {
    var session = new MockHttpSession();
    given(authProvider.start(eq(AuthMethod.SMART_ID), any(), any()))
        .willReturn(new AuthProvider.Challenge("login-4", "9012", null));
    mockMvc.perform(
        post("/v1/auth/smart-id")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"personalCode":"60001017869"}"""));

    mockMvc
        .perform(delete("/v1/auth/logins/login-4").session(session))
        .andExpect(status().isNoContent());

    verify(authProvider).cancel("login-4");
  }

  @Test
  void isAnonymousWithoutALogin() throws Exception {
    mockMvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
  }
}
