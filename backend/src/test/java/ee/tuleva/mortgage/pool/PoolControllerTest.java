package ee.tuleva.mortgage.pool;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PoolController.class)
@Import(PoolConfiguration.class)
class PoolControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private PoolStatsService poolStatsService;

  @Test
  void servesAggregatePoolStats() throws Exception {
    var first = new PoolStats.MilestoneStatus(1000, new BigDecimal("1.35"), false);
    given(poolStatsService.currentStats())
        .willReturn(
            new PoolStats(
                520L,
                new BigDecimal("60000000"),
                1000,
                false,
                OffsetDateTime.parse("2026-11-10T00:00:00+02:00"),
                List.of(first),
                null,
                first));

    mockMvc
        .perform(get("/v1/pool/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.joinedCount").value(520))
        .andExpect(jsonPath("$.committedVolume").value(60000000))
        .andExpect(jsonPath("$.minimumReached").value(false))
        .andExpect(jsonPath("$.nextMilestone.members").value(1000))
        .andExpect(jsonPath("$.nextMilestone.margin").value(1.35))
        .andExpect(jsonPath("$.currentMilestone").isEmpty());
  }

  @Test
  void servesPublicConfigurationFromApplicationYaml() throws Exception {
    mockMvc
        .perform(get("/v1/config/public"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.calculator.euribor6m").value(2.936))
        .andExpect(jsonPath("$.calculator.defaultMargin").value(1.70))
        .andExpect(jsonPath("$.pool.deadline").value("2026-11-10T00:00:00+02:00"))
        .andExpect(jsonPath("$.pool.minimumMembers").value(1000))
        .andExpect(jsonPath("$.pool.milestones.length()").value(3))
        .andExpect(jsonPath("$.pool.milestones[0].margin").value(1.35))
        .andExpect(jsonPath("$.pool.milestones[2].margin").value(1.20));
  }
}
