package ee.tuleva.mortgage.auth;

import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class AuthConfiguration {

  @Bean
  NameDirectory nameDirectory(AuthProperties properties, Clock clock) {
    return properties.nameLookup().enabled()
        ? new LdapNameDirectory(properties.nameLookup(), clock)
        : personalCode -> Optional.empty();
  }

  @Bean
  AuthProvider authProvider(AuthProperties properties, NameDirectory names, Clock clock) {
    return new StubAuthProvider(properties, names, clock);
  }
}
