package ee.tuleva.mortgage.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class LdapNameDirectoryTest {

  private final LdapNameDirectory directory =
      new LdapNameDirectory(
          new AuthProperties.NameLookup(
              true, Duration.ofSeconds(1), 2, Duration.ofMinutes(1), List.of()),
          Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC));

  @Test
  void onlyExactPersonalCodesAreLookedUp() {
    assertThat(directory.find("*")).isEmpty();
    assertThat(directory.find("6000101786")).isEmpty();
    assertThat(directory.withinRateLimit()).as("rejected codes do not count").isTrue();
  }

  @Test
  void stopsLookingNamesUpAfterTheRateLimit() {
    assertThat(directory.withinRateLimit()).isTrue();
    assertThat(directory.withinRateLimit()).isTrue();
    assertThat(directory.withinRateLimit()).isFalse();
  }
}
