package ee.tuleva.mortgage.auth;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The demo login: Smart-ID and Mobile-ID are emulated, the name comes from the public eID
 * certificate directories.
 *
 * @param stubCodeDisplay how long the verification code is shown, as a person would take to compare
 *     it with their phone and enter their PIN
 * @param stubVerifying how long the "verifying" step after the code takes
 */
@ConfigurationProperties(prefix = "app.auth")
record AuthProperties(Duration stubCodeDisplay, Duration stubVerifying, NameLookup nameLookup) {

  record NameLookup(
      boolean enabled,
      Duration timeout,
      int maxLookups,
      Duration window,
      List<Directory> directories) {}

  record Directory(String url, String base) {}
}
