package ee.tuleva.mortgage.auth;

import java.util.Optional;

/** Finds a person's name by their Estonian personal code. */
interface NameDirectory {

  Optional<PersonName> find(String personalCode);
}
