package ee.tuleva.mortgage;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  @Test
  void verifiesModuleStructure() {
    ApplicationModules.of(MortgagePoolApplication.class).verify();
  }
}
