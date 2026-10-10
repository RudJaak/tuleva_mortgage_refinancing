package ee.tuleva.mortgage.demo;

import java.time.Clock;
import java.util.random.RandomGenerator;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DemoProperties.class)
class DemoConfiguration {

  @Bean
  @ConditionalOnBooleanProperty("app.demo.seed-enabled")
  ApplicationRunner demoSeedRunner(DemoSeeder seeder, DemoProperties properties) {
    return _ -> seeder.seedIfEmpty(properties.seedBorrowers(), properties.seedVolume());
  }

  @Bean
  @ConditionalOnBooleanProperty("app.demo.simulated-joiners.enabled")
  SimulatedJoiners simulatedJoiners(
      DemoProperties properties, TaskScheduler taskScheduler, Clock clock) {
    return new SimulatedJoiners(
        properties.simulatedJoiners(), taskScheduler, clock, RandomGenerator.getDefault());
  }
}
