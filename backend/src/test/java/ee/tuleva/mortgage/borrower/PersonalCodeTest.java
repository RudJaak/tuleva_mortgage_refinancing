package ee.tuleva.mortgage.borrower;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PersonalCodeTest {

  @ParameterizedTest
  @ValueSource(strings = {"40504040001", "60001017869", "39901012239"})
  void acceptsValidCodes(String code) {
    assertThat(PersonalCode.isValid(code)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "40504040002",
        "38888888888",
        "4050404000",
        "405040400011",
        "abcdefghijk",
        "79901012239"
      })
  void rejectsWrongChecksumLengthOrCentury(String code) {
    assertThat(PersonalCode.isValid(code)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"39913012236", "39902302231"})
  void rejectsImpossibleBirthDates(String code) {
    assertThat(PersonalCode.isValid(code)).isFalse();
  }

  @Test
  void readsTheBirthDate() {
    assertThat(PersonalCode.birthDate("60001017869")).isEqualTo(LocalDate.of(2000, 1, 1));
  }
}
