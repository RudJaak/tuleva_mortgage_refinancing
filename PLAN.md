# PLAN

_Status: **scope frozen for the initial build** (2026-10-10). Changes from here on are recorded as new decisions._

## Goal

Build a **borrower-facing** app that:
1. **Persuades** Estonian mortgage holders that joining a pooled refinancing tender is worth it. Teammates own the content; we render it.
2. **Collects** what's needed to put them in the pool, with minimal friction, and ends in a **signed commitment to join**. **This is our focus.**

## Decisions

| # | Decision | Date |
|---|---|---|
| D1 | **Joining = a signed, conditional commitment**, not a non-binding interest form (see "Commitment model" below) | 2026-10-09 |
| D2 | **LLM-based document extraction is in scope** and is the core frictionless feature. A Claude API key will be provided later; build behind an interface with a stub until then | 2026-10-09 |
| D3 | **Borrower flow only.** No bank-facing pool dashboard or export in the demo. The data model should still be pool- and export-ready (ESMA-style field naming) | 2026-10-09 |
| D4 | The repo is **public**. `docs/inputs/` and root temp files are gitignored and must never be committed. Synthetic data only | 2026-10-09 |
| D5 | Stack matches Tuleva: Java/Spring Boot + PostgreSQL/Flyway; React/TypeScript | 2026-10-09 |
| D6 | Timebox 1–2 days. Use real integrations where feasible, with mocks behind the same interfaces otherwise | 2026-10-09 |
| D7 | The landing page has a **savings calculator in Tuleva's style**, reusing their design language and UX. After auth and document upload, the same calculation **reruns on validated numbers**. The only remaining unknown is the negotiated margin, shown as **worst, expected and best case** scenarios | 2026-10-10 |
| D8 | The landing page has a **live pool tracker**. See "Landing page" below | 2026-10-10 |
| D9 | The tender is triggered by **a deadline and/or a pool-size target** | 2026-10-10 |
| D10 | Joining Tuleva is **not** part of the funnel | 2026-10-10 |
| D11 | The UI is **Estonian only**. Code and docs stay in English | 2026-10-10 |
| D12 | **Tuleva has authorised full reuse** of its code, website theme, calculators, design and logo (the team is part of Tuleva for the hackathon). The demo **runs locally** | 2026-10-10 |
| D13 | **Must-have documents: the loan contract and the 6-month account statement**, with PDF parsing as the MVP and the fallback for every bank. Lower-friction statement channels are nice-to-have and currently being researched | 2026-10-10 |
| D14 | **One commitment threshold for the whole pool**: the target margin M at pool close. A borrower can only join if M is below their own margin | 2026-10-10 |
| D15 | Authorising the 90-day early-repayment notice is a **stretch goal**, not in the initial scope | 2026-10-10 |
| D16 | Missed deadline: re-authorise against an extended deadline. **Not in scope** for now | 2026-10-10 |
| D17 | **Persuasion content** will arrive as facts, statistics and slogans that we render in a well-designed layout. For now we build **placeholders** | 2026-10-10 |
| D18 | **Margin ladder values come from teammates.** They will be somewhat better than the placeholders | 2026-10-10 |
| D19 | **Co-borrowers are not in the initial scope.** We only record that a co-borrower exists | 2026-10-10 |
| D20 | **Upload priorities** — **Must:** loan contract (LLM extraction), statement as plain PDF (LLM + balance reconciliation), statement as **bank-sealed ASiC-E/BDOC** (verify the seal with digidoc4j, then parse the inner file). **Should:** camt.053 XML and CSV statements. **Stretch:** "connect your bank" via Enable Banking AIS (needs a sandbox account). **Could:** EMTA tax-data certificate and home-insurance policy. Every channel reduces to the same derived summary; raw transactions are discarded | 2026-10-10 |
| D21 | **Up-front fields:** identity; old-loan facts incl. contract number; property (energy class auto-filled from EHR); household (co-borrower flag, marital status, property regime); other liabilities; 6 months of statements. **After a bank is chosen:** valuation, early-repayment application, notary | 2026-10-10 |
| D22 | **Placeholder tracker values:** deadline **31.12.2026**; margin ladder €25M → 1.45%, €50M → 1.35%, €100M → 1.25% (margins superseded by D23). Both live in config and can change | 2026-10-10 |
| D23 | **Ladder tiers = calculator scenarios:** €25M → **1.30%** (Tagasihoidlik), €50M → **1.20%** (Eesmärk), €100M → **1.10%** (Parim). Still placeholders until teammates confirm | 2026-10-10 |
| D24 | **Milestones replace the D23 tiers.** **€100M is the minimum**: below it no tender happens. Tiers are €100M → **1.20%** (Tagasihoidlik), €300M → **1.00%** (Eesmärk), €500M → **0.80%** (Parim). Above €100M the pool keeps growing until the deadline, and the highest milestone reached sets the target margin M | 2026-10-10 |
| D25 | **Tracker counts signed commitments only**; withdrawals subtract. The demo seed starts **mid-way to €100M** (~€60M, ~520 synthetic borrowers) | 2026-10-10 |
| D26 | **Statement extraction: the LLM categorises, code calculates.** Claude returns the statement frame plus only the screening-relevant transactions, each with a category (salary, other income, mortgage, consumer loan, leasing, hire purchase, credit card, child support, late fee, debt collection, enforcement, gambling, savings). All totals, averages and flags are computed in code. Obligations use the median monthly instalment, with outliers listed as one-off payments. Credit-card repayments are card usage, not debt service. A statement longer than six months is analysed over its last six full calendar months, one ending more than 45 days ago is flagged as stale, and other income counts as regular only when the same payer pays in at least three months. No full-statement reconciliation: a bank seal already proves integrity, and plain PDFs stay indicative. **The bank seal is not required for the demo** (`REQUIRE_BANK_SEAL=false`): a missing or untrusted seal lowers the verification level instead of rejecting the upload | 2026-10-10 |

## Non-goals for the hackathon

- Running the tender, making credit offers, ESIS, binding bank decisions. See `docs/research/05-legal-regulatory.md`.
- A bank or back-office pool view and export (D3).
- A licensed-intermediary compliance stack, servicing, the fund or bond structure.
- PSD2 production access, EMTA X-tee, bank APIs.

## Commitment model (D1)

Why commit at all? Incumbent banks can match any outside offer, and about 40% of mortgage "new business" is already renegotiation with the borrower's own bank (`01-market.md`). A pool of soft interest has little leverage. A pool of **committed volume** is what banks would bid for.

**The model:** the borrower signs a **conditional commitment** with Smart-ID / Mobile-ID. **The threshold is one value for the whole pool (D14).**

> "If the joint tender produces an offer with a margin of at most **M%** (the pool's target margin at close), with switching costs covered or at most **€Y**, I will move my loan to the winning bank."

- **M is the pool's margin ladder step at the moment the pool closes.** Everyone in the pool commits to the same number, which is what banks bid against.
- **Eligibility guard:** a borrower can only join if M is below their own validated current margin. Joining has to save them money. The calculator shows their personal saving at M.
- **Withdrawal:** free before the tender closes. After the result, there is a cooling-off window to accept or decline the concrete offer. The ESIS and the bank's own credit check still happen at that point.
- **What gets signed:** the intent to switch, plus a consent package:
  - share pseudonymised data in the tender;
  - release identifiable data only to the winning bank.
- **Not legally a loan contract.** It's a signed declaration of intent, or mandate. It is weakly enforceable by design. Its value is signalling to banks and adding friction to backing out.
- **Legal flag:** collecting signed switching mandates is even more clearly **mortgage credit intermediation** (KAVS § 4). The demo should label the operator "Tuleva or a licensed partner (TBD)".
- **Stretch goal (D15):** authorise the 90-day early-repayment notice to the current bank as part of signing.

**Tender trigger (D9, D24):** the pool closes on the deadline (31.12.2026). It must have reached the **€100M minimum** by then, or no tender happens. The highest milestone reached sets M.

Open points:
- **Out of scope (D19):** co-borrower signing. In production, every borrower on the loan must sign.
- **Out of scope (D16):** what happens if the deadline is missed. The direction is to re-authorise everyone against an extended deadline.

## Landing page (D7, D8)

**1. Savings calculator.** Port of Tuleva's calculator UX; see `docs/research/08-tuleva-design-and-code.md`.
- **Inputs:** remaining balance, remaining term, current margin (or "I don't know", which defaults to ~1.7%). The current bank is optional.
- **Outputs:** savings per month, over the remaining term and as NPV, under three scenarios:
  - The three scenarios are the three ladder tiers (D24): **Tagasihoidlik** (modest) = tier 1, **Eesmärk** (goal) = tier 2, **Parim** (best) = tier 3.
  - All scenario values live in config.
- **After login and upload,** the same component re-renders with **validated** balance, term and margin. The only variable left is the tender margin, still shown as worst / expected / best. Mark validated values ("from your contract").

**2. Pool tracker (live counter and progress bar).**
- Shows the **number of people who have joined**, their **combined remaining balance**, and the **target volume €N** with a progress bar and the deadline D.
- **Margin ladder:** the target margin falls as the pool grows. The milestone markers sit on the progress bar, and the "expected" scenario in the calculator follows the current ladder step. **Placeholders only.** Teammates will provide the real values (D18), and they will be somewhat better than these. Keep them in config:

  | Pool reaches | Target margin |
  |---|---|
  | €100M (minimum) | 1.20% |
  | €300M | 1.00% |
  | €500M | 0.80% |

- **Data:** a public, aggregate-only API endpoint (count, sum of balances, current ladder step). It must never return per-person data. For the demo, seed it with synthetic commitments.
- **Compliance wording** (Reklaamiseadus § 29): show these as **targets or goals, not offers**. For example, use "Eesmärk" (goal) rather than "Pakkumine" (offer), and add a disclaimer that the final terms depend on the tender and each bank's credit decision.

## Phases

| # | Phase | Status |
|---|---|---|
| 0 | Context: research, CLAUDE.md, this plan | ✅ done |
| 1 | Scope grooming: freeze the journey, fields and integrations | ✅ done |
| 2–5 | Build: broken into tasks in `TASKS.md` | ⏳ now |

## Borrower journey

Each stage asks only for what the next decision needs. Anything an upload can provide is pre-filled; the user only confirms it.

**Stage 0: Landing page (no login).**
- Placeholder sections for the teammates' facts, statistics and slogans (D17), plus the savings calculator and the pool tracker (see "Landing page").
- Headline comparison: Estonia ≈1.55 pp over Euribor vs Finland ≈0.5 pp.
- The calculator inputs carry over into the flow after login.

**Stage 1: Your loan (login, about 2 minutes). Upload first.**
1. Log in with Smart-ID / Mobile-ID. This gives isikukood, name and age.
2. **Upload the loan contract, or the bank's payoff statement (VÕS § 411(7)), or a screenshot of the internet bank.** The LLM extracts bank, balance, margin, Euribor tenor, maturity, monthly payment and any fixed-rate period. The user confirms or edits each field, and each field is tagged with its source.
3. Property: address autocomplete (In-ADS), then building data from Ehitisregister (type, area, year, energy class). The user gives an estimated value.
4. Household: number of adults and dependants, whether there is a co-borrower (flag only, D19), marital status and property regime (needed for spouse consent at the notary).
5. Wishes: keep the term, extend it, or merge loans.

Result: the **indicative savings for *their* loan**, and their indicative risk class.

**Stage 2: Verify (the same session, or come back later).**
- Upload **6 months of account statements**. Extract net income, existing loan and leasing payments, and arrears signals. Raw transactions are not stored; only the derived figures are.
- Optionally upload the **EMTA tax-data certificate** for income and the **land-register extract** (free for owners) for lien holder, amount and rank.
- Result: a verified risk class, with each field marked "declared" or "document-verified".

**Stage 3: Commit.**
- Review a summary of all data, your personal saving at the pool's target margin M (D14), and the consents. Then **sign with Smart-ID / Mobile-ID**.
- Confirmation screen: "You're in pool #N, with €M committed so far" (synthetic numbers) and what happens next.

**Out of scope:** expert valuation, the ESIS, the bank's decision, the actual switch.

## Integrations (priority for a 1–2 day build)

| Integration | Value | Feasibility | Priority |
|---|---|---|---|
| Smart-ID / Mobile-ID **auth + signing** (SK demo env) | Identity; signing the commitment | High; Tuleva's `onboarding-service` already implements auth | **Must** |
| **LLM document extraction** (contract, payoff statement, account statements; Claude API) | Pre-fills the hard fields; verifiable data | High; key comes later, so stub first | **Must** (D2) |
| In-ADS address autocomplete | Clean address, ADS IDs | High | **Must** |
| Ehitisregister building data | Type, area, year, energy class | Medium; API to verify | Should |
| Maa-amet price statistics (AVM hint) | Indicative LTV | Medium | Could |
| PSD2 AIS sandbox | Statements without uploads | Medium-low | Stretch |
| e-kinnistusraamat | Lien data | Paid; terms risk | Via user upload only |
| EMTA X-tee | Verified income | Not feasible now | Post-hackathon |

**Extraction design notes**
- Use structured output: a JSON schema per document type, with a confidence score and the source page or snippet for each field.
- The user always confirms. Never auto-submit.
- Delete originals after extraction unless the user opts in to keeping them for the bank.
- Minimise statement data: send only what's needed and store only aggregates. Bank transactions can reveal special-category data (EDPB 06/2020).
- Synthetic sample documents for the demo: an Estonian-style loan contract, a payoff statement and a statement CSV/PDF. **Never use real ones.**

## Risk class (borrower-facing, indicative)

Use transparent rules, not ML, so the result is explainable. Thresholds live in config.

- **Gates:** no arrears in the last 12 months; balance ≥ ~€50k; remaining term ≥ ~10 years.
- **Dimensions:** LTV band, DSTI at max(contract rate, 6%), income stability, region, energy class, verification level.
- **Output:** A / B / C / not eligible yet, with reasons and "how to improve" hints (e.g. "verify income to move from B to A").
- It is informational only, with no decision about the person (GDPR Art 22).

## Research completed (2026-10-10)

1. ✅ Refinancing documents → `docs/research/06-refinancing-documents.md`. Key point: banks expect **bank-sealed statements (.asice/.bdoc)**; the valuation is ordered and paid for by the borrower and is only needed before signing; collect marital status and property regime up front.
2. ✅ Account-statement channels → `docs/research/07-account-data-access.md`. The ladder: Enable Banking AIS (covers all 9 EE banks; free sandbox; demo with your own accounts) > sealed ASiC-E upload (Swedbank, Coop) > camt.053 XML > CSV > PDF+LLM (MVP) > screenshot.
3. ✅ Tuleva's calculator UX, design system, licences, and auth/signing → `docs/research/08-tuleva-design-and-code.md`. All resources are authorised for reuse (D12). Signing (DigiDoc4j) can be reused for the commitment.

## Open questions (remaining)

1. **Target €N and the final margin ladder:** values to come from teammates. Placeholders are in D22.
2. **Bank-connection approach** (the stretch goal): the user is evaluating it. See `07-account-data-access.md`, "Aggregator vs direct bank APIs".
