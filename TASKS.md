# TASKS

Build backlog for phases 2–5. The scope and decisions it implements (D1–D22) are in `PLAN.md`. The research behind them is in `docs/research/`.

**Time available:** about 1.5 days, until the hackathon ends on 11 Oct 2026. Estimates are for a single developer working with Claude Code.

**Rule:** every external dependency (eID, Claude API, In-ADS, EHR, Enable Banking) sits behind an interface, and each interface has a **stub implementation that works with no network or keys**. The demo must always be runnable end to end on stubs. Real integrations replace stubs one at a time.

## Lanes (can run in parallel once the skeleton exists)

| Lane | Focus | Tasks |
|---|---|---|
| **A: Platform** | repo, backend skeleton, auth, signing | T01–T06, T40–T42 |
| **B: Frontend** | landing page, calculator, tracker, flow screens | T10–T15, T30–T34, T43 |
| **C: Documents** | upload pipeline, extraction, statements | T20–T27 |
| **D: Domain** | property lookups, risk class, pool stats, synthetic data | T16–T17, T35–T38, T50–T51 |

## Milestones and cut line

| Milestone | What it shows | Must be done by |
|---|---|---|
| **M1 Walking skeleton** | Landing page with a working calculator and tracker (seeded), Mobile-ID demo login, empty flow screens | Sat midday |
| **M2 Loan from document** | Upload the contract → extracted fields → confirm → personal savings on validated numbers | Sat evening |
| **M3 Verified borrower** | Statement upload (PDF + sealed ASiC-E) → income and liabilities summary → risk class | Sun morning |
| **M4 Commitment** | Review page → sign with Smart-ID/Mobile-ID → tracker goes up | Sun midday |
| **M5 Demo-ready** | Polish, content placeholders filled, demo script rehearsed | Before pitch |

**If behind schedule, drop in this order:** T27 (Enable Banking) → T25 (CSV) → T24 (camt.053) → T17 (EHR) → T42 (real ASiC-E signing; fall back to T41's auth-as-confirmation) → T23 (ASiC-E statement verification). **Never drop:** M2 (contract extraction) and the calculator/tracker. They carry the pitch.

---

## Phase 2: Skeleton

### T01 Repo scaffolding · A · 1h · ✅ done
- Monorepo layout: `backend/`, `frontend/`, `docker-compose.yml` (Postgres 17), root `README.md` with run instructions.
- `.githooks/pii-scan` pre-commit hook, adapted from Tuleva's onboarding-service: blocks isikukood-like numbers outside an allowlist of SK demo codes, emails, `.env`, and anything under `docs/inputs/`.
- `.env.example` with `ANTHROPIC_API_KEY=`, `EXTRACTION_MODE=stub|claude`, `AUTH_MODE=stub|sk-demo`.
- **Done when:** `docker compose up -d` and `./gradlew bootRun` start (`npm run dev` follows in T10), and the hook rejects a test commit containing a fake isikukood.
- **Outcome:** the hooks were copied from Tuleva's `.githooks` (pii-scan with isikukood mod-11 / IBAN / email / name-roster detection), with our own allowlist of SK demo codes. `.env` is imported by Spring.

### T02 Backend skeleton · A · 1.5h · after T01 · ✅ done
- Spring Boot 4.1 / Java 25 / Gradle KTS, following Tuleva's conventions: Spring Modulith packages, Flyway, Spotless, JUnit 5. Spock is optional.
- Planned modules: `auth`, `signature`, `borrower`, `loan`, `document`, `statement`, `property`, `risk`, `pool`, `config`.
- `GET /actuator/health`. A CORS setup for the Vite dev server (`/v1/**`, origin from `app.cors.allowed-origins`).
- **Done when:** the app boots against Postgres, Flyway runs `V1__init.sql`, and one slice test passes.
- **Outcome:** `ee.tuleva.mortgage` on Boot 4.1.1 / Modulith 2.1.1, with Spotless (google-java-format). Tests: `HealthEndpointTests` (Testcontainers PG 18, Flyway + health UP) and `ModularityTests`. The module packages get created as their tasks land.

### T03 Data model · D · 1.5h · after T02
- Flyway migrations. Use ESMA-style column comments (RREL*/RREC*) where they map.
- `borrower`: id, personal code (encrypted at rest or hashed + last 4 shown), name, birth date, marital status, property regime, co-borrower flag, adults, dependants.
- `loan`: lender, contract number, balance, margin, euribor tenor, maturity date, monthly payment, fixed-period end, mortgage amount and rank, wish (keep / extend / merge). **Each field has `{value, source: DECLARED|DOCUMENT|REGISTER, document_id, confidence, confirmed_at}`.** Store this as JSONB per field, or as a sibling `field_provenance` table.
- `property`: ADS id, address, cadastral number, registry part number, type, area, year built, energy class, estimated value, region.
- `liability`: type, monthly payment, source.
- `statement_summary`: months covered, monthly net income (avg/min), recurring loan and lease payments, arrears and overdraft flags, reconciliation status, verification level (PLAIN_PDF | SEALED | AIS).
- `document`: type, format, sha256, seal-verified flag, status, deleted_at. **No raw file kept after extraction by default.**
- `commitment`: borrower, pool id, target margin M at signing, signed container reference, signed_at, withdrawn_at.
- `pool`: id, deadline, target volume, ladder (JSON).
- **Done when:** the migrations apply cleanly and repository slice tests pass.

### T04 Auth: Mobile-ID + Smart-ID (SK demo) · A · 3h · after T02
- Copy from `TulevaEE/onboarding-service` (MIT; reuse authorised): `auth/mobileid`, `auth/smartid`, `config/MobileIdConfiguration`, `SmartIdClientConfiguration`, `SmartIdTrustedCaCertificates`, the demo certificates, and the truststore.
- Strip out EPIS, user, AML and other dependencies.
- **Mobile-ID first:** pure REST, no callback, so it's the easiest locally. Then **Smart-ID via the notification flow by personal code**. The device-link callback needs https, so skip it unless we tunnel.
- Issue our own session JWT.
- `AUTH_MODE=stub` logs in as a fixed demo person.
- **Done when:** demo Mobile-ID `+37268000769` / `60001017869` and Smart-ID `40504040001` log in, show the verification code in the UI, and return a session with name and birth date.

### T05 Extraction interface + stub · C · 1h · after T02
- `DocumentExtractor` with `extract(DocumentType, bytes, mime) → ExtractionResult<T>`, where each field carries `{value, confidence, sourcePage, snippet}`.
- `StubExtractor` returns fixtures matching the synthetic documents (T50).
- Switched by `EXTRACTION_MODE`.
- **Done when:** a slice test gets typed results from the stub for `LOAN_CONTRACT` and `ACCOUNT_STATEMENT`.

### T06 Config: pool, ladder, scenarios · D · 0.5h · after T02
- `application.yml`:
  - pool deadline `2026-12-31`, target volume, ladder `[25M→1.45, 50M→1.35, 100M→1.25]` (D22);
  - calculator worst and best margins; default margin 1.70%; 6M Euribor anchor 2.936%; NPV discount 3%; switching cost €400;
  - risk-class thresholds.
- `GET /v1/config/public` exposes what the frontend needs.
- **Done when:** the frontend can read the config, and changing the YAML changes the UI.

### T10 Frontend skeleton · B · 2h · after T01 · ✅ done
- Vite + React + TS + Bootstrap 5.3.
- **Tuleva SCSS**: `_variables.scss` and fonts (Roboto, Merriweather) from onboarding-client; logo from wordpress-theme, with an attribution note.
- react-intl with `et` only (all strings in `translations.et.json`), react-router, react-query, react-hook-form.
- Feature folders: `src/components/<feature>/`.
- Routes: `/` landing; `/sisene` login; `/laen`, `/vara`, `/leibkond`, `/valjavotted`, `/kinnitus` flow steps; `/valmis` done.
- **Done when:** the routes render with Tuleva styling at 1280 and 390 px.
- **Outcome:**
  - Kept the current Vite template: Vite 8, **React 19**, TS 6, react-router 7, TanStack Query 5, Vitest + RTL, oxlint, Prettier.
  - Tuleva SCSS, the Roboto font and the logo are copied (MIT). Merriweather comes from Google Fonts.
  - The flow stepper collapses to "Samm n/5" on mobile.
  - The dev proxy to :9000 is verified. 10 route tests pass. Screenshots were checked at 1280 and 390 px.

---

## Phase 3: Landing page and collection flow

### T11 Annuity / savings library · B · 1.5h · after T10 · ✅ done
- Pure TypeScript `calc/` module:
  - annuity payment;
  - remaining interest at current vs target margin;
  - monthly saving, lifetime saving, NPV (3%), break-even with a switching cost.
- Unit tests, including the reference case from `02-teammate-market-analysis.md`: €125k, 20 years, 2.5% + 1.85% → €780.73/month.
- **Done when:** the tests pass. This is the single source of the calculation maths; the backend doesn't duplicate it.
- **Outcome:** `frontend/src/calc/savings.ts`: `annuityPayment`, `totalInterest`, `compareMargins`. The reference case reproduces exactly (780.73 / 747.63 / 33.09 / 7,942.69 / NPV 5,567.31 / break-even 12.1). NPV discounts monthly at the annual rate ÷ 12.

### T12 Savings calculator component · B · 3h · after T11, T06 · ✅ done
- A port of Tuleva's calculator card (wordpress-theme `front-hero/calculator.php`, `js/calculator.js`, `_custom-slider.scss`):
  - inputs: balance (€), remaining years, margin (slider with an "Eesti keskmine" marker, plus an "Ei tea" toggle);
  - rows: "Praegu maksad" vs "Ühispakkumisega" for worst, expected and best, with "Säästaksid X €" in green.
- The **expected** case follows the current ladder step from T13's API.
- `validated` mode: inputs are prefilled from the loan, with "✓ lepingust" badges; the only variable is the scenario.
- Inputs persist to `sessionStorage` and carry into the flow.
- **Done when:** it works anonymously on the landing page and in validated mode on the review step.
- **Outcome:**
  - `components/calculator/`: a port of Tuleva's card, segmented scenario buttons, and a margin slider with a value bubble and a "Uute laenude keskmine ~1,6%" marker. The "Ei tea" option falls back to 1.70%.
  - Results show per month, until the end of the loan, NPV and break-even, with the compliance disclaimer. Inputs persist in `sessionStorage`. `validated` mode shows "✓ lepingust" badges.
  - **Settings are static defaults for now** (`settings.ts` → `useCalculatorSettings`). T06 and T13 must feed them from `/v1/config/public` and `/v1/pool/stats`; the "expected" case = the next unreached ladder step.
  - The review step (T34) still has to mount it in validated mode.

### T13 Pool tracker: API + component · D+B · 3h · after T03, T06, T10
- **Backend:** `GET /v1/pool/stats` returns `{joinedCount, committedBalance, targetVolume, deadline, ladder[], currentStep, nextStep}`.
  - Aggregates only. Clamp small counts to avoid re-identification: hide or round the balance when fewer than ~10 people have joined.
- **Frontend:**
  - a big Merriweather counter;
  - a Bootstrap progress bar with ladder milestone markers;
  - a "Eesmärk: marginaal X%" label;
  - a flip-tile countdown to 31.12.2026, ported from wordpress-theme `countdown-timer.php` / `_counter.scss`;
  - the compliance disclaimer (§ 29: goal, not offer).
- Polls every 30 s.
- **Done when:** the seeded data shows, and a new commitment (T42) moves the bar.

### T14 Landing page assembly · B · 2h · after T12, T13
- Hero with headline (Estonia ≈1.55 pp vs Finland ≈0.5 pp over Euribor; ECB MIR Aug 2026), calculator, tracker, "Kuidas see töötab" (how it works, 3 steps), placeholder blocks for teammates' facts, statistics and slogans (D17), FAQ placeholder, CTA "Liitu ühispakkumisega" (join the joint offer).
- Content comes from one `content.et.json` so teammates can edit it without touching code.
- **Done when:** the page is responsive and every content slot is fed from JSON.

### T15 Flow shell + login screen · B · 2h · after T04, T10
- Login with Mobile-ID (phone + personal code) and Smart-ID (personal code). Show the verification code and poll for status.
- Stepper for the flow steps; save and resume (progress kept server-side).
- **Done when:** the demo accounts log in and land on `/laen`.

### T20 Upload pipeline · C · 3h · after T05, T03
- `POST /v1/documents` (multipart).
- **Format sniffing:**
  - ASiC-E/BDOC zip mimetype → T23;
  - XML with a camt namespace → T24;
  - CSV → T25;
  - PDF or image → LLM.
- sha256 dedupe, size limit, async status (`GET /v1/documents/{id}`).
- Original bytes are deleted after extraction unless kept by opt-in.
- **Done when:** the stub extraction runs end to end and the original bytes are gone afterwards.

### T21 Claude extractor: loan contract · C · 3h · after T20
- Claude API with a native PDF document block. Structured output via a tool or JSON schema.
- Fields: lender, contract number, contract date, original amount, **balance** (if present), **margin**, **Euribor tenor**, rate type, fixed period, maturity, monthly payment, mortgage amount, early-repayment terms. Each field carries confidence, page and snippet.
- Also handles a payoff statement or an internet-bank screenshot (image block), using the same schema with partial fields.
- Model ID and parameters come from config. **Check the `claude-api` skill for the current model ID and PDF and structured-output usage before implementing.**
- **Done when:** the synthetic contract (T50) extracts with every key field correct, and `EXTRACTION_MODE=stub` still works.

### T22 Statement extraction (PDF) + summary · C · 4h · after T20
- **LLM:** extract the opening and closing balance, period, and transactions.
- **Code:** reconcile (opening + Σ = closing, ±€0.01), then derive the summary:
  - **net income:** recurring credits, salary-like descriptors ("töötasu", employer name), monthly avg and min;
  - **recurring loan and lease payments:** debits to banks or leasing companies, "laen", "liising";
  - **arrears and overdraft signals:** negative balance, "viivis".
- **Persist only the summary**; drop the transactions. Rules first, with the LLM as a fallback for categorisation.
- **Done when:** the synthetic statement reconciles and the summary matches the expected fixture. A tampered PDF that breaks reconciliation is flagged.

### T23 Sealed statement (ASiC-E/BDOC) · C · 2h · after T20, (T40 for digidoc4j setup)
- Validate the container with digidoc4j: signature or seal valid, signer CN = a known bank, list of data files.
- Route the inner file (PDF, XML or CSV) to the right parser.
- Set `verification_level = SEALED` with the signer name.
- **Done when:** a test container validates, and an altered one fails. The demo container is either a team member's own Swedbank or Coop export (kept private, in `docs/inputs/`, never committed) or a test-signed one.

### T24 camt.053 parser · C · 1.5h · after T22 · *Should*
- Parse balances (OPBD/CLBD) and entries for 001.02 and 001.08, feeding the same summary logic as T22.
- Reconciliation is exact.

### T25 CSV mappers · C · 2h · after T22 · *Should*
- Mappers for LHV (the new 16-column format), Swedbank and SEB. Handle delimiter and decimal sniffing.

### T27 Enable Banking AIS · C · 4h · *Stretch*
- JWT client, `GET /aspsps?country=EE`, `/auth` redirect, `/sessions`, then immediately fetch 6 months of transactions into the T22 summary.
- Warn that the bank may ask for a second confirmation for history older than 90 days.
- **Blocked on:** a sandbox account and the user's evaluation of the approach.

### T30 Loan step UI `/laen` · B · 3h · after T15, T20
- Upload first, with a drag-and-drop zone showing accepted formats ("Laenuleping, väljavõte laenujäägist või ekraanipilt"), then a progress state.
- Confirm form: each field is pre-filled with a source badge (lepingust / sisestatud) and a low-confidence highlight; the user edits or confirms.
- Manual-entry fallback.
- Wish: keep the term, extend it, or merge.
- **Done when:** upload → confirmed loan saved with provenance.

### T31 Property step UI `/vara` · B+D · 2.5h · after T15, T16
- In-ADS autocomplete. Building data from EHR (T17) shown as read-only chips. Estimated value field.
- Region is derived (Harjumaa vs others).
- **Done when:** a real Tallinn address resolves to an ADS id and building data.

### T16 In-ADS address proxy · D · 1.5h · after T02
- Backend proxy to the Maa- ja Ruumiamet In-ADS gazetteer: address search → ADS id, normalised address, coordinates, cadastral number where available.
- Stub with a few fixed addresses.
- **Verify the current endpoint and terms first.**

### T17 EHR building data · D · 2h · after T16 · *Should*
- Ehitisregister public API: from ADS / EHR code to building type, area, year built, **energy class** (from the energy certificate).
- Stub fallback.
- **Verify the endpoint first.**

### T32 Household & liabilities step `/leibkond` · B · 2h · after T15
- Fields: adults, dependants, co-borrower flag (D19), marital status, property regime (ühisvara / lahusvara / vara lahusus abieluvaraleping), employment type, other loans, leases and guarantees.
- Pre-filled from the statement summary when available.

### T33 Statements step `/valjavotted` · B · 2.5h · after T15, T22
- Multi-file upload for "all accounts, 6 months". Show a short how-to per bank: where to download, and **a recommendation to choose the digitally sealed (.asice) export**.
- Show the summary per account plus a combined total, with reconciliation and seal badges.
- If T27 is done, a "Ühenda pank" (connect bank) button.

---

## Phase 4: Risk class and commitment

### T35 Risk-class engine · D · 3h · after T03, T06
- Pure rules over the stored data, with thresholds in config (PLAN.md "Risk class").
- **Gates:** no arrears, balance ≥ €50k, remaining term ≥ 10 years.
- **Dimensions:** LTV band, DSTI at max(contract rate, 6%), income stability, region, energy class, verification level.
- **Output:** A / B / C / not eligible yet, with reasons and improvement hints. It is informational only (GDPR Art 22).
- Table-driven unit tests.
- **Done when:** `GET /v1/me/risk-class` returns the class with reasons and hints.

### T36 Eligibility guard + personal saving · D · 1h · after T35, T11
- The borrower can commit only if their validated margin is greater than M, the pool's current target (D14).
- Expose the personal saving at M; the frontend computes it with T11, the backend re-checks it.

### T34 Review & commit step `/kinnitus` · B · 3h · after T30–T33, T35
- Summary of all data with source badges.
- Risk class card.
- Calculator in **validated mode** (T12).
- **Commitment text** with M, the €Y cost cap, withdrawal rights and the operator "Tuleva või litsentseeritud partner (täpsustamisel)".
- Granular consent checkboxes:
  - pool participation;
  - pseudonymised sharing with banks;
  - identifiable data to the winning bank only;
  - contact.
- Sign button.

### T40 Signing setup (digidoc4j) · A · 2h · after T04
- Copy `signature/**` and `DigiDocConfiguration` (TEST mode) from onboarding-service.
- Build an ASiC-E with the commitment PDF or text.
- **Done when:** a container is created and its data-to-sign computed.

### T41 Fallback: auth-as-confirmation · A · 1h · after T04
- If T42 slips, "signing" becomes a fresh Mobile-ID or Smart-ID authentication over a hash of the commitment text. Store the hash, the auth certificate and a timestamp.
- Label it honestly in the UI and pitch as "kinnitatud" (confirmed), not "allkirjastatud" (signed).

### T42 Commitment signing with Smart-ID / Mobile-ID · A · 3h · after T40, T34
- `PUT /v1/commitments/{id}/signature/{smart-id|mobile-id}` and `GET …/status`, following Tuleva's `SignatureController` / `MandateController`.
- On success: store the container reference, update the pool aggregate, offer a downloadable .asice.
- **Done when:** the demo account signs, the container validates in DigiDoc4j, and the tracker count goes up.

### T43 Done page `/valmis` · B · 1h · after T42
- "Oled ühispakkumises" (you're in the joint offer): pool step, people joined, committed volume, deadline countdown.
- "Mis edasi" (what next): tender after 31.12.2026, then the offer, then your decision, then the valuation and notary.
- Withdraw link: withdrawal is free before the deadline.

---

## Phase 5: Data, polish, demo

### T50 Synthetic documents · C · 2h · early (needed by T05, T21, T22)
- Generate realistic Estonian-language **fake** documents with fictional names and SK demo personal codes:
  - a loan contract PDF (Swedbank-like layout, but no logo);
  - a 6-month statement PDF with salary, utilities, a loan payment and a leasing payment;
  - a camt.053 XML;
  - an LHV-style CSV.
- Keep them in `fixtures/` (committed, synthetic only) together with their expected-extraction JSON.

### T51 Pool seed data · D · 1h · after T03
- A Flyway repeatable migration or a dev-profile seeder that creates ~300 synthetic commitments totalling ~€32M, which puts the pool past step 1.
- Includes a "reset demo" endpoint, active only in the dev profile.

### T52 Content integration · B · 1h · when teammates deliver
- Drop the teammates' facts, statistics and slogans into `content.et.json`. Check the wording against § 29: goals, not offers.

### T53 Demo script + pitch support · all · 2h · M5
- A 3-minute click-path:
  1. landing page and calculator;
  2. "Liitu";
  3. Mobile-ID;
  4. upload the contract and see the fields fill;
  5. property autocomplete;
  6. upload a sealed statement and get "✓ panga digitempel" (bank's digital seal);
  7. risk class A;
  8. validated savings;
  9. sign;
  10. the tracker moves.
- A backup screen recording in case the network or SK demo environment fails.
- `README.md`: how to run, and the architecture sketch.

### T54 Quality pass · all · 2h · M5
- Check 390 px mobile, Estonian copy (diacritics, NBSPs), empty, error and loading states, and that stub mode runs end to end.
- `git log` and `pii-scan` are clean before any push.

---

## API sketch (for parallel frontend and backend work)

```
GET  /v1/config/public
GET  /v1/pool/stats
POST /v1/auth/mobile-id            GET /v1/auth/mobile-id/status
POST /v1/auth/smart-id             GET /v1/auth/smart-id/status
GET  /v1/me                        (borrower + progress)
POST /v1/documents                 GET /v1/documents/{id}        (status + extraction)
PUT  /v1/me/loan                   PUT /v1/me/property           PUT /v1/me/household
GET  /v1/me/statements/summary
GET  /v1/address/search?q=         GET /v1/buildings/{adsId}
GET  /v1/me/risk-class
POST /v1/commitments               PUT /v1/commitments/{id}/signature/{method}
GET  /v1/commitments/{id}/signature/{method}/status
POST /v1/commitments/{id}/withdraw
```

## Critical path

```
T01 → T02 → T05 → T20 → T21 ─┐
          ↘ T04 → T15 → T30 ─┴→ M2 → T22/T23 → T33 → T35 → T34 → T40/T42 → M4
T10 → T11 → T12 ─→ T13 → T14 → M1
```
