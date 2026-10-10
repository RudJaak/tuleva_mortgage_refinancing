package ee.tuleva.mortgage.demo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DemoProperties.class)
class DemoConfiguration {

  @Bean
  @ConditionalOnBooleanProperty("app.demo.seed-enabled")
  ApplicationRunner demoSeedRunner(DemoSeeder seeder, DemoProperties properties) {
    return args -> seeder.seedIfEmpty(properties.seedBorrowers(), properties.seedVolume());
  }
}
