package ee.tuleva.mortgage.auth;

import java.util.UUID;

/** Published when a borrower completes a login. */
public record BorrowerLoggedIn(UUID borrowerId) {}
