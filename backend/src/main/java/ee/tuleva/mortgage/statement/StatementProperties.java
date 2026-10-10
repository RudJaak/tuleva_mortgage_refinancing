package ee.tuleva.mortgage.statement;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.statement")
record StatementProperties(List<TrustedBank> trustedBanks) {

  record TrustedBank(String name, String registryCode) {}

  Optional<TrustedBank> bankWithRegistryCode(String registryCode) {
    return trustedBanks.stream()
        .filter(bank -> bank.registryCode().equals(registryCode))
        .findFirst();
  }
}
