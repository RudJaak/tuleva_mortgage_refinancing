package ee.tuleva.mortgage.auth;

import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * SK's certificate directory (esteid.ldap.sk.ee) only offers TLS 1.2 with RSA key exchange
 * (AES256-GCM-SHA384), which Java disables by default as {@code TLS_RSA_*}. The disabled list is
 * JVM-wide and cannot be relaxed per connection, so this re-allows just that family before any TLS
 * connection is made. Every other restriction stays; servers offering forward-secret suites (the
 * Claude API, Zetes' directory) still negotiate those, and TLS 1.2's handshake integrity prevents a
 * downgrade. Without it, names of people with ID cards made before November 2025 or Mobile-ID
 * cannot be looked up.
 */
public final class EidDirectoryTls {

  static final String PROPERTY = "jdk.tls.disabledAlgorithms";
  static final String RSA_KEY_EXCHANGE = "TLS_RSA_*";

  private EidDirectoryTls() {}

  /** Must run before the first TLS connection: the JDK reads the property once. */
  public static void allowRsaKeyExchange() {
    String disabled = Security.getProperty(PROPERTY);
    if (disabled != null) {
      Security.setProperty(PROPERTY, without(disabled, RSA_KEY_EXCHANGE));
    }
  }

  static String without(String disabledAlgorithms, String entry) {
    return Arrays.stream(disabledAlgorithms.split(","))
        .map(String::strip)
        .filter(algorithm -> !algorithm.equals(entry))
        .collect(Collectors.joining(", "));
  }
}
