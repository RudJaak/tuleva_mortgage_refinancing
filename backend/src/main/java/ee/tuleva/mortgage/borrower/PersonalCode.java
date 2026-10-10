package ee.tuleva.mortgage.borrower;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

public final class PersonalCode {

  private static final Pattern ELEVEN_DIGITS = Pattern.compile("\\d{11}");
  private static final Map<Character, Integer> CENTURY =
      Map.of('1', 1800, '2', 1800, '3', 1900, '4', 1900, '5', 2000, '6', 2000);
  private static final int[] FIRST_WEIGHTS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
  private static final int[] SECOND_WEIGHTS = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

  private PersonalCode() {}

  public static boolean isValid(String code) {
    if (code == null || !ELEVEN_DIGITS.matcher(code).matches()) {
      return false;
    }
    Integer century = CENTURY.get(code.charAt(0));
    return century != null && hasRealBirthDate(code, century) && hasValidChecksum(code);
  }

  private static boolean hasRealBirthDate(String code, int century) {
    try {
      LocalDate.of(
          century + Integer.parseInt(code.substring(1, 3)),
          Integer.parseInt(code.substring(3, 5)),
          Integer.parseInt(code.substring(5, 7)));
      return true;
    } catch (DateTimeException e) {
      return false;
    }
  }

  private static boolean hasValidChecksum(String code) {
    int remainder = weightedRemainder(code, FIRST_WEIGHTS);
    if (remainder == 10) {
      remainder = weightedRemainder(code, SECOND_WEIGHTS);
      remainder = remainder == 10 ? 0 : remainder;
    }
    return remainder == code.charAt(10) - '0';
  }

  private static int weightedRemainder(String code, int[] weights) {
    int sum = 0;
    for (int i = 0; i < weights.length; i++) {
      sum += (code.charAt(i) - '0') * weights[i];
    }
    return sum % 11;
  }
}
