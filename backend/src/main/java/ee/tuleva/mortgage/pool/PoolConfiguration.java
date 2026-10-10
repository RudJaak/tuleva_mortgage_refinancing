package ee.tuleva.mortgage.pool;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PoolProperties.class, CalculatorProperties.class})
class PoolConfiguration {}
