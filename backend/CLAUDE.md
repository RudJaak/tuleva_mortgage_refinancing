# backend/CLAUDE.md

This is the Spring Boot API for the borrower flow. Project context and decisions are in `../CLAUDE.md`, `../PLAN.md` and `../TASKS.md`.

## Stack

- Java 25, Spring Boot 4.1.1, Gradle Kotlin DSL (use the wrapper `./gradlew`; there's no global Gradle).
- **Spring Modulith 2.1.1.** Each top-level package under `ee.tuleva.mortgage` is a module, and sub-packages are internal to it. `ModularityTests` enforces this.
- Planned modules: `auth`, `signature`, `borrower`, `loan`, `document`, `statement`, `property`, `risk`, `pool`, `config`.
- PostgreSQL 18 (via `../docker-compose.yml`), Flyway migrations in `src/main/resources/db/migration`, JPA with `ddl-auto: validate`.
- **Jackson 3** (`tools.jackson.*`, not `com.fasterxml.jackson.*`). Test annotations live in the new Boot 4 packages, e.g. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` and `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.
- Modules so far:
  - `config`: CORS, Clock;
  - `borrower`;
  - `pool`: commitments, stats, public config;
  - `demo`: seeder, resets (whole demo, or one borrower's flow, also on every login) and simulated joiners;
  - `document`: `POST /v1/documents`, container intake and bank-seal check (digidoc4j), the `DocumentProcessor` hook;
  - `extraction`: shared Claude client (`ClaudeDocumentReader`: PDF in, structured output out);
  - `statement`: account-statement extraction (stub or Claude), analysis and summary;
  - `loan`: loan-contract extraction with per-field evidence, derived figures and screening flags;
  - `commitment`: the review step's emulated signing, which creates the pool commitment.

  Cross-module references use IDs (UUID), not JPA relations.
- Runs on port **9000**. The Vite dev server is on 5173 and is allowed by CORS on `/v1/**`.

## Conventions

These are adapted from Tuleva's onboarding-service so the code can be ported later.

- **Formatting:** Spotless with google-java-format. Run `./gradlew spotlessApply` before committing.
- **Tests:**
  - JUnit 5 + AssertJ.
  - Prefer slice tests (`@WebMvcTest`, `@DataJpaTest`) over `@SpringBootTest`.
  - Full-context tests import `TestcontainersConfiguration` (Postgres 18). Docker must be running.
  - Write the test first where practical.
- **Code style:**
  - **Records** for every DTO, response body, config class (`@ConfigurationProperties`) and value type.
  - **Lombok** where records aren't possible:
    - JPA entities: `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PRIVATE)` plus static factory methods;
    - services and controllers: `@RequiredArgsConstructor`;
    - logging: `@Slf4j`.
  - **No hand-written getters or constructors.**
  - Package-private by default. Inject `Clock` instead of calling `now()`. Avoid comments that restate the code.
  - Column names come from the default snake_case naming strategy, so no `@Column(name=…)` is needed.
- **Flyway:** migrations are append-only, `V<n>__<snake_case>.sql`. Never edit an applied migration. While `bootRun` is running, devtools applies a new migration as soon as it compiles, so finish a migration before saving it or stop the app first.
- **API:** REST under `/v1/...`, JSON. The API sketch is in `../TASKS.md`.
- **Login** is emulated (D29): `auth` shows a verification code and looks the name up in the public eID certificate directories over LDAPS. `EidDirectoryTls` re-allows RSA key exchange at startup because SK's directory offers nothing else.
- **External integrations** (Claude, Enable Banking):
  - Each one sits behind an interface with a **stub** implementation, selected by `app.<area>.mode` (`EXTRACTION_MODE` in `../.env`).
  - Stub mode must work offline.
- **Copied Tuleva code** (onboarding-service is MIT; reuse is authorised): keep package-level attribution in a `package-info.java` or a short header comment naming the source path.

## Personal data (this repo is PUBLIC)

- No real personal codes, names, emails or IBANs in code, tests or fixtures.
  - Use SK demo identities, which are listed in `../.githooks/pii-idcode-allow`.
  - Use synthetic data.
- **Persist the minimum:**
  - Statement transactions are reduced to a summary, and raw transactions are discarded.
  - Uploaded originals are deleted after extraction.
- **Never log** request bodies, extracted document text, or personal codes.

## Commands

```bash
docker compose -f ../docker-compose.yml up -d
./gradlew bootRun
./gradlew test
./gradlew spotlessApply
```
