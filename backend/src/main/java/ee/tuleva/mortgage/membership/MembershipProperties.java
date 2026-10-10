package ee.tuleva.mortgage.membership;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** The emulated Tuleva membership check (D32). */
@ConfigurationProperties(prefix = "app.membership")
record MembershipProperties(boolean stubMember, Duration stubDelay) {}
