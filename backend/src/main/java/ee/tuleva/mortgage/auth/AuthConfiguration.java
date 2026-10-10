package ee.tuleva.mortgage.auth;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class AuthConfiguration {

  @Bean
  @ConditionalOnProperty(name = "app.auth.mode", havingValue = "stub", matchIfMissing = true)
  AuthProvider stubAuthProvider(AuthProperties properties, Clock clock) {
    return new StubAuthProvider(properties, clock);
  }
}
