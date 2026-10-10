package ee.tuleva.mortgage.borrower;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersonNamesTest {

  @Test
  void theSameNameIgnoresOrderCaseAccentsAndMiddleNames() {
    assertThat(PersonNames.sameName("TAMM, JAAN", "Jaan Tamm")).isTrue();
    assertThat(PersonNames.sameName("Mari-Liis Õunapuu", "MARI LIIS OUNAPUU")).isTrue();
    assertThat(PersonNames.sameName("Jaan Peeter Tamm", "Jaan Tamm")).isTrue();
    assertThat(PersonNames.sameName("Mari Tamm", "Jaan Tamm")).isFalse();
    assertThat(PersonNames.sameName(null, "Jaan Tamm")).isFalse();
  }
}
