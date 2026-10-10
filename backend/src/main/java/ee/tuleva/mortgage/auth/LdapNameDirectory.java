package ee.tuleva.mortgage.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Looks names up in the public eID certificate directories, the ones DigiDoc4 uses to find
 * someone's encryption certificate: SK ID Solutions (ID cards made before November 2025, Digi-ID,
 * residence cards, Mobile-ID) and Zetes (Thales ID cards since November 2025). Both answer exact
 * personal codes only. Smart-ID certificates are not published, so a Smart-ID-only person is not
 * found. Names and codes are never logged; a rate limit keeps the demo from becoming a name oracle.
 * See docs/research/10-name-from-personal-code.md.
 */
@Slf4j
class LdapNameDirectory implements NameDirectory {

  private final List<AuthProperties.Directory> directories;
  private final AuthProperties.NameLookup settings;
  private final Clock clock;
  private final Deque<Instant> recentLookups = new ArrayDeque<>();

  LdapNameDirectory(AuthProperties.NameLookup settings, Clock clock) {
    this.directories = settings.directories();
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  public Optional<PersonName> find(String personalCode) {
    if (!personalCode.matches("\\d{11}") || !withinRateLimit()) {
      return Optional.empty();
    }
    for (var directory : directories) {
      try {
        Optional<PersonName> name = search(directory, personalCode);
        if (name.isPresent()) {
          return name;
        }
      } catch (NamingException e) {
        log.info("Name lookup failed in {}: {}", directory.url(), e.getClass().getSimpleName());
      }
    }
    return Optional.empty();
  }

  private Optional<PersonName> search(AuthProperties.Directory directory, String personalCode)
      throws NamingException {
    var environment = new Hashtable<String, String>();
    environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    environment.put(Context.PROVIDER_URL, directory.url());
    environment.put(Context.SECURITY_AUTHENTICATION, "none");
    String timeout = String.valueOf(settings.timeout().toMillis());
    environment.put("com.sun.jndi.ldap.connect.timeout", timeout);
    environment.put("com.sun.jndi.ldap.read.timeout", timeout);

    var context = new InitialLdapContext(environment, null);
    try {
      var controls = new SearchControls();
      controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      controls.setReturningAttributes(new String[] {"cn"});
      controls.setCountLimit(50);
      var results =
          context.search(
              directory.base(),
              "(serialNumber={0})",
              new Object[] {"PNOEE-" + personalCode},
              controls);
      while (results.hasMore()) {
        SearchResult result = results.next();
        var cn = result.getAttributes().get("cn");
        if (cn != null) {
          var name = PersonName.fromCertificateCn(String.valueOf(cn.get()));
          if (name.isPresent()) {
            return name;
          }
        }
      }
      return Optional.empty();
    } finally {
      context.close();
    }
  }

  synchronized boolean withinRateLimit() {
    Instant now = clock.instant();
    while (!recentLookups.isEmpty()
        && recentLookups.peekFirst().isBefore(now.minus(settings.window()))) {
      recentLookups.pollFirst();
    }
    if (recentLookups.size() >= settings.maxLookups()) {
      log.warn("Name lookup rate limit reached");
      return false;
    }
    recentLookups.addLast(now);
    return true;
  }
}
