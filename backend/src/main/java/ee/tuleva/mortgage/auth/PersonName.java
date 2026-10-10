package ee.tuleva.mortgage.auth;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** A person's name as printed in their eID certificate, in normal letter case. */
record PersonName(String firstName, String lastName) {

  private static final Locale ESTONIAN = Locale.forLanguageTag("et");

  /**
   * Reads a certificate CN of the form {@code SURNAME,GIVEN NAMES,PERSONALCODE}, e.g. {@code
   * ÕUNAPUU,MARI LIIS,40404040009} gives "Mari Liis" and "Õunapuu".
   */
  static Optional<PersonName> fromCertificateCn(String cn) {
    String[] parts = cn.split(",");
    if (parts.length < 3 || parts[0].isBlank() || parts[1].isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new PersonName(titleCase(parts[1]), titleCase(parts[0])));
  }

  /** "MARI-LIIS" → "Mari-Liis", "O'CONNOR" → "O'Connor", spaces normalised. */
  static String titleCase(String name) {
    return Arrays.stream(name.strip().split("\\s+"))
        .map(PersonName::titleCaseWord)
        .collect(Collectors.joining(" "));
  }

  private static String titleCaseWord(String word) {
    StringBuilder result = new StringBuilder(word.length());
    boolean startOfPart = true;
    for (int offset = 0; offset < word.length(); ) {
      int codePoint = word.codePointAt(offset);
      String letter = new String(Character.toChars(codePoint));
      result.append(startOfPart ? letter.toUpperCase(ESTONIAN) : letter.toLowerCase(ESTONIAN));
      startOfPart = codePoint == '-' || codePoint == '\'' || codePoint == '’';
      offset += Character.charCount(codePoint);
    }
    return result.toString();
  }
}
