package ee.tuleva.mortgage.borrower;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Matches a name printed on a document to a borrower, ignoring case, accents and order. */
public final class PersonNames {

  public enum Match {
    MATCH,
    MISMATCH,
    NOT_CHECKED
  }

  private PersonNames() {}

  public static Match match(
      @Nullable String printedName, @Nullable String firstName, @Nullable String lastName) {
    if (printedName == null || printedName.isBlank() || firstName == null || lastName == null) {
      return Match.NOT_CHECKED;
    }
    Set<String> printed = tokens(printedName);
    return printed.containsAll(tokens(firstName)) && printed.containsAll(tokens(lastName))
        ? Match.MATCH
        : Match.MISMATCH;
  }

  public static String normalise(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toUpperCase(Locale.ROOT)
        .strip();
  }

  private static Set<String> tokens(String name) {
    return Arrays.stream(normalise(name).split("[\\s,.-]+"))
        .filter(token -> !token.isEmpty())
        .collect(Collectors.toSet());
  }
}
