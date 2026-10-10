package ee.tuleva.mortgage.document;

import static org.digidoc4j.Configuration.Mode.PROD;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
class DocumentConfiguration {

  // Adapted from TulevaEE/onboarding-service config/DigiDocConfiguration (MIT).
  // Bank seals are real production certificates, so this always validates against the EE
  // production trusted list, even when eID signing runs against the SK demo environment.
  @Bean
  org.digidoc4j.Configuration documentDigiDocConfiguration() {
    var configuration = new org.digidoc4j.Configuration(PROD);
    configuration.setTrustedTerritories("EE");
    return configuration;
  }

  /** Runs extraction after the upload has been answered; each document gets a virtual thread. */
  @Bean(destroyMethod = "shutdown")
  ExecutorService documentProcessingExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
