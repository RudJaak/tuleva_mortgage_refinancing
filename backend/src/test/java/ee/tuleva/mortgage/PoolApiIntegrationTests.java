package ee.tuleva.mortgage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.demo.seed-enabled=true", "app.demo.simulated-joiners.enabled=false"})
@AutoConfigureTestRestTemplate
class PoolApiIntegrationTests {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void seededDemoPoolIsMidWayToTheMinimum() {
    var stats = restTemplate.getForObject("/v1/pool/stats", JsonNode.class);

    assertThat(stats.get("joinedCount").asLong()).isEqualTo(520);
    assertThat(stats.get("committedVolume").decimalValue()).isEqualByComparingTo("60000000");
    assertThat(stats.get("minimumReached").asBoolean()).isFalse();
    assertThat(stats.get("nextMilestone").get("margin").decimalValue())
        .isEqualByComparingTo("1.20");
  }

  @Test
  void resetKeepsTheSyntheticPool() {
    var reset = restTemplate.postForObject("/v1/demo/reset", null, JsonNode.class);
    var stats = restTemplate.getForObject("/v1/pool/stats", JsonNode.class);

    assertThat(reset.get("removedBorrowers").asInt()).isZero();
    assertThat(stats.get("joinedCount").asLong()).isEqualTo(520);
  }
}
