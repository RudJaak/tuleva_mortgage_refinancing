package ee.tuleva.mortgage.statement;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param recencyChecks flag statements that end too long ago and analyse only the last six months
 *     of a longer one; off only to study old statements during development
 */
@ConfigurationProperties(prefix = "app.statements")
record StatementProperties(boolean recencyChecks) {}
