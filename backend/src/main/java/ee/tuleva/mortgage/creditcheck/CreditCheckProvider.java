package ee.tuleva.mortgage.creditcheck;

/**
 * Asks a payment-default register about a person. In a real launch a licensed partner would query
 * Creditinfo (docs/research/11); the demo uses a stub.
 */
interface CreditCheckProvider {

  record Report(int activeDefaults, String source) {}

  Report check(String personalCode);
}
