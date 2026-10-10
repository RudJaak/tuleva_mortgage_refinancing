package ee.tuleva.mortgage.statement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatementProcessorTest {

  @Test
  void showsOnlyTheCountryAndTheLastFourDigitsOfTheAccount() {
    assertThat(StatementProcessor.mask("EE12 3456 7890 1234 5678")).isEqualTo("EE…5678");
    assertThat(StatementProcessor.mask(null)).isNull();
  }
}
