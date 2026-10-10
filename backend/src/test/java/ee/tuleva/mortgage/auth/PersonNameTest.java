package ee.tuleva.mortgage.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PersonNameTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "ÕUNAPUU,MARI LIIS,60001017869 | Mari Liis | Õunapuu",
        "MAASIKAS,MARI-LIIS,60001017869 | Mari-Liis | Maasikas",
        "O'CONNOR-TAMM,JAAN,39901012239 | Jaan | O'Connor-Tamm",
        "ÄÄRELAID,  ÜLLE   ÖIE ,40404040009 | Ülle Öie | Äärelaid",
        "ŠAŠKOV,ŽANNA,40404040009 | Žanna | Šaškov"
      })
  void readsTheNamesFromACertificateCn(String cn, String firstName, String lastName) {
    assertThat(PersonName.fromCertificateCn(cn)).contains(new PersonName(firstName, lastName));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {"60001017869", "TAMM,,60001017869", ",JAAN,60001017869"})
  void anUnexpectedCnGivesNoName(String cn) {
    assertThat(PersonName.fromCertificateCn(cn)).isEmpty();
  }
}
