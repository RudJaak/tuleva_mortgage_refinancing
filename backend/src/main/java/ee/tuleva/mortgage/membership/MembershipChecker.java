package ee.tuleva.mortgage.membership;

/** Whether a person is a Tuleva member; a real one would ask Tuleva's member register. */
interface MembershipChecker {

  boolean isMember(String personalCode);
}
