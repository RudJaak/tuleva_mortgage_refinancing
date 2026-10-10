package ee.tuleva.mortgage.statement;

import static org.digidoc4j.Configuration.Mode.PROD;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StatementProperties.class)
class StatementConfiguration {

  // Adapted from TulevaEE/onboarding-service config/DigiDocConfiguration (MIT).
  // Bank seals are real production certificates, so this always validates against the EE
  // production trusted list, even when eID signing runs against the SK demo environment.
  @Bean
  org.digidoc4j.Configuration statementDigiDocConfiguration() {
    var configuration = new org.digidoc4j.Configuration(PROD);
    configuration.setTrustedTerritories("EE");
    return configuration;
  }
}
