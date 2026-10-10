package ee.tuleva.mortgage.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.calculator")
public record CalculatorProperties(
    BigDecimal euribor6m,
    LocalDate euriborDate,
    BigDecimal defaultMargin,
    BigDecimal marketAverageMargin,
    BigDecimal switchingCost,
    BigDecimal annualDiscountRate,
    BigDecimal membershipFee) {}
