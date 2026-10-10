# TASKS

Build backlog for phases 2–5. The scope and decisions it implements (D1–D22) are in `PLAN.md`. The research behind them is in `docs/research/`.

**Time available:** about 1.5 days, until the hackathon ends on 11 Oct 2026. Estimates are for a single developer working with Claude Code.

**Rule:** every external dependency (eID, Claude API, In-ADS, EHR, Enable Banking) sits behind an interface, and each interface has a **stub implementation that works with no network or keys**. The demo must always be runnable end to end on stubs. Real integrations replace stubs one at a time.

## Lanes (can run in parallel once the skeleton exists)

| Lane | Focus | Tasks |
|---|---|---|
| **A: Platform** | repo, backend skeleton, auth, signing | T05 |
| **B: Frontend** | landing page, calculator, tracker, flow screens | T10–T15, T30, T33 |
| **C: Documents** | upload pipeline, extraction, statements | T20–T27 |
| **D: Domain** | property lookups, risk class, pool stats, synthetic data | T35–T38, T50–T51 |

## Milestones and cut line

| Milestone | What it shows | Must be done by |
|---|---|---|
| **M1 Walking skeleton** | Landing page with a working calculator and tracker (seeded), emulated Smart-ID / Mobile-ID login, empty flow screens | Sat midday |
| **M2 Loan from document** | Upload the contract → extracted fields → confirm → personal savings on validated numbers | Sat evening |
| **M3 Verified borrower** | Statement upload (PDF + sealed ASiC-E) → income and liabilities summary → risk class | Sun morning |
| **M4 Commitment** | Review page → sign with Smart-ID/Mobile-ID → tracker goes up | Sun midday |
| **M5 Demo-ready** | Polish, content placeholders filled, demo script rehearsed | Before pitch |

**If behind schedule, drop in this order:** T27 (Enable Banking) → T25 (CSV) → T24 (camt.053). **Never drop:** M2 (contract extraction) and the calculator/tracker. They carry the pitch.

---

## Phase 2: Skeleton

### T03 Data model · D · 1.5h · after T02 · 🟡 partly done
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
- **Outcome (part 1):**
  - `V2__borrower_and_commitment.sql`. `borrower` has a nullable personal code and a `synthetic` flag. `commitment` stores the committed balance (RREL30), the current margin (RREL46) and a `SIGNED`/`WITHDRAWN` status, with one signed commitment per borrower.
  - **The other tables (loan, property, liability, statement_summary, document, field provenance) are added with the tasks that use them** (T20, T22, T30), so we don't design them before they're needed.

### T14 Landing page assembly · B · 2h · after T12, T13
- Hero with headline (Estonia ≈1.55 pp vs Finland ≈0.5 pp over Euribor; ECB MIR Aug 2026), calculator, tracker, "Kuidas see töötab" (how it works, 3 steps), FAQ placeholder, CTA "Liitu ühispakkumisega" (join the joint offer).
- **Done when:** the page is responsive at 1280 px and 390 px.

### T20 Upload pipeline · C · 3h · after T05, T03 · 🟡 core done
- Done: `POST /v1/documents?type=…` (multipart, logged-in borrower) in the `document` module. PDF and ASiC-E/BDOC are sniffed, the container is opened and its seal checked, metadata is stored in `document`, and the files go to the `DocumentProcessor` registered for the type. The bytes are never stored.
- Processing is asynchronous: the POST answers 202 after the seal check (~1 s), extraction runs on a virtual thread, and `GET /v1/documents?type=…` returns status and result. Results are held in memory only.
- Remaining: sha256 dedupe, camt.053 and CSV sniffing (T24, T25).
### T21 Claude extractor: loan contract · C · 3h · after T20 · 🟡 built, waiting for a real contract
- Done: `loan` module. `POST /v1/documents?type=LOAN_CONTRACT` reads every field with page, quote and confidence (`ClaudeLoanContractExtractor` or the stub); `LoanAnalyzer` derives the current rate, remaining term, an estimated balance, age at maturity and screening flags; stored in `loan_contract` (V4). Signed contract containers are verified per signer, including whether the borrower signed.
- Remaining: run against a real contract and tune the prompt; payoff statement and internet-bank screenshot inputs.
- Original scope:
- Claude API with a native PDF document block. Structured output via a tool or JSON schema.
- Fields: lender, contract number, contract date, original amount, **balance** (if present), **margin**, **Euribor tenor**, rate type, fixed period, maturity, monthly payment, mortgage amount, early-repayment terms. Each field carries confidence, page and snippet.
- Also handles a payoff statement or an internet-bank screenshot (image block), using the same schema with partial fields.
- Model ID and parameters come from config. **Check the `claude-api` skill for the current model ID and PDF and structured-output usage before implementing.**
- **Done when:** the synthetic contract (T50) extracts with every key field correct, and `EXTRACTION_MODE=stub` still works.

### T22 Statement extraction (PDF) + summary · C · 4h · after T20 · 🟡 works, more banks to try
- Done (D26): `statement` module. `ClaudeStatementExtractor` (Claude reads the PDF and returns categorised relevant transactions) and `StubStatementExtractor`; `StatementAnalyzer` derives income, obligations, debt-service ratio, savings, holder-name match and flags; only the summary is stored in `statement_summary`.
- Processed documents reload their stored result after a restart.
- Verified on a real LHV statement (~30 s, ~28k input tokens). Obligations use the median instalment and list outliers as one-off payments; credit-card repayments are reported as card usage, outside obligations and the debt-service ratio.
- Remaining: try statements from other banks and tune the prompt.
### T24 camt.053 parser · C · 1.5h · after T22 · *Should*
- Parse balances (OPBD/CLBD) and entries for 001.02 and 001.08, feeding the same summary logic as T22.
- Reconciliation is exact.

### T25 CSV mappers · C · 2h · after T22 · *Should*
- Mappers for LHV (the new 16-column format), Swedbank and SEB. Handle delimiter and decimal sniffing.

### T27 Enable Banking AIS · C · 4h · *Stretch*
- JWT client, `GET /aspsps?country=EE`, `/auth` redirect, `/sessions`, then immediately fetch 6 months of transactions into the T22 summary.
- Warn that the bank may ask for a second confirmation for history older than 90 days.
- **Blocked on:** a sandbox account and the user's evaluation of the approach.

### T30 Loan step UI `/laen` · B · 3h · after T15, T20 · 🟡 done except payoff statement and screenshot inputs
- Done: `/laen` uploads the contract (signer badge, progress), pre-fills a confirm form with the document, page and quote behind every field (low-confidence fields marked), saves through `PUT /v1/me/loan` with per-field provenance (DOCUMENT or DECLARED, V5 `loan` table), then shows the current rate, balance, remaining term, LTV and flags with the savings calculator on the confirmed numbers. Manual entry works without a contract.
- Remaining:
- Upload first, with a drag-and-drop zone showing accepted formats ("Laenuleping, väljavõte laenujäägist või ekraanipilt"), then a progress state.
- Confirm form: each field is pre-filled with a source badge (lepingust / sisestatud) and a low-confidence highlight; the user edits or confirms.
- Manual-entry fallback.
- Wish: keep the term, extend it, or merge.
- **Done when:** upload → confirmed loan saved with provenance.

### T33 Statements step `/valjavotted` · B · 2.5h · after T15, T22 · 🟡 single upload done
- Done: upload on `/valjavotted` with an instant seal badge, an estimated progress bar with stage messages while Claude reads (the borrower can move on), and a summary card per statement.
- Done (D28): several files per upload (picker or drag-and-drop) with instructions to add every account; statements grouped per person (holder matched to the contract's borrowers), co-borrowers asked for theirs, per-person and household totals (`GET /v1/me/statements`, V6 holder and account columns, each account counted once).
- Remaining:
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

---

## Phase 5: Data, polish, demo

### T50 Synthetic documents · C · 2h · early (needed by T05, T21, T22) · 🟡 loan contract done
- Done: `tools/synthetic-documents/generate-contract.mjs` makes a NÄIDIS loan contract and its ESIS for any name and figures; `fixtures/loan-contract/` holds a fictional one with its expected extraction (read correctly by Claude on 2026-10-10).
- Remaining:
- Generate realistic Estonian-language **fake** documents with fictional names and SK demo personal codes:
  - a loan contract PDF (Swedbank-like layout, but no logo);
  - a 6-month statement PDF with salary, utilities, a loan payment and a leasing payment;
  - a camt.053 XML;
  - an LHV-style CSV.
- Keep them in `fixtures/` (committed, synthetic only) together with their expected-extraction JSON.

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

## API (as built)

```
GET  /v1/config/public                 GET  /v1/pool/stats
POST /v1/auth/{smart-id|mobile-id}     GET  /v1/auth/logins/{id}      DELETE /v1/auth/logins/{id}
GET  /v1/me                            POST /v1/auth/logout
POST /v1/documents?type=…              GET  /v1/documents?type=…
GET  /v1/me/loan                       PUT  /v1/me/loan
GET  /v1/me/statements
GET  /v1/me/commitment                 POST /v1/me/commitment/signing
GET  /v1/me/commitment/signing/{id}    DELETE /v1/me/commitment/signing/{id}
POST /v1/demo/flow/reset               POST /v1/demo/reset
GET  /v1/me/risk-class                 (T35, not built yet)
```

## Critical path

Done: login (D29), contract → loan step (M2), statements (M3 minus the risk class), review and emulated signing (M4).

```
T35 (risk class) → T36 (eligibility) → M5 (T14 landing page, T52, T53, T54)
```
