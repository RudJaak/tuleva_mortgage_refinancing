# tuleva_mortgage_refinancing

Tuleva hackathon (9–11 Oct 2026), idea **L6: Kodulaenude ühine refinantseerimisfond**.

Estonian mortgage holders join a pool, upload their loan contract and account statements, and sign a conditional commitment. A joint tender then lets banks bid for the whole pool's refinancing.

- **Context and decisions:** [`CLAUDE.md`](CLAUDE.md), [`PLAN.md`](PLAN.md)
- **Backlog:** [`TASKS.md`](TASKS.md)
- **Research:** [`docs/research/`](docs/research)

## Repository layout

```
backend/    Spring Boot 4 / Java 25 API (Gradle). See backend/CLAUDE.md
frontend/   React + TypeScript (Vite). See frontend/CLAUDE.md
docs/       Research notes (docs/inputs/ is private and gitignored)
.githooks/  Personal-data scanner and hooks. This repo is PUBLIC
```

## First-time setup

```bash
git config core.hooksPath .githooks   # mandatory: blocks personal data from being committed
cp .env.example .env                  # optional: defaults run everything on offline stubs
```

**Requirements:** JDK 25, Docker, Node 22+.

## Run locally

```bash
docker compose up -d                  # Postgres on :5432
cd backend && ./gradlew bootRun       # API on :9000, health at /actuator/health
cd frontend && npm install && npm run dev   # app on :5173, proxies /v1 to :9000
```

## Test

```bash
cd backend && ./gradlew test spotlessCheck   # tests use Testcontainers (Docker must be running)
cd backend && ./gradlew spotlessApply        # format the code
.githooks/pii-scan-test                      # self-test for the personal-data scanner
cd frontend && npm test && npm run lint && npx tsc -b
```

## Integration modes

Every external integration has an offline stub. You switch between them in `.env`:

| Variable | Values | Notes |
|---|---|---|
| `AUTH_MODE` | `stub` / `sk-demo` | `sk-demo` uses SK's public Smart-ID / Mobile-ID DEMO environment |
| `EXTRACTION_MODE` | `stub` / `claude` | `claude` needs `ANTHROPIC_API_KEY` |
| `REQUIRE_BANK_SEAL` | `false` / `true` | `true` rejects uploads that aren't sealed by a known bank. With `false`, they're accepted at a lower verification level |
| `STATEMENT_RECENCY_CHECKS` | `true` / `false` | `false` only for studying old statements: no staleness flag, and a long statement is analysed whole instead of over its last 6 months |
| `DEMO_SEED` | `true` / `false` | Seeds a synthetic pool (~520 borrowers, ~€60M) on startup. `POST /v1/demo/reset` removes everything signed during a demo |
| `DEMO_FRESH_LOGIN` | `true` / `false` | Every login clears that borrower's documents and loan, so a demo starts over (the stub login always signs in the same person). The flow also has an "Alusta algusest" link |

## Attribution

Parts of this project are adapted from Tuleva's open-source repositories, with Tuleva's permission:

- [onboarding-service](https://github.com/TulevaEE/onboarding-service) (MIT)
- [onboarding-client](https://github.com/TulevaEE/onboarding-client) (MIT)
- [wordpress-theme](https://github.com/TulevaEE/wordpress-theme)

See `.githooks/NOTICE` and per-file notes.
