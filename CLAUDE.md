# CLAUDE.md

Hackathon project for the **Tuleva hackathon, 9–11 Oct 2026**, idea **L6 "Kodulaenude ühine refinantseerimisfond"** (joint home-loan refinancing fund). The idea was submitted by Loit Linnupõld.

**Working language is English.** This applies to code, docs, commits and answers. **The end-user UI is Estonian only.** Sources may be in Estonian.

## The idea in one paragraph

Estonian mortgage borrowers each negotiate with their bank alone and rarely refinance. Market concentration, switching costs and poor comparability keep margins above what a large pooled portfolio would command. Estonia's margins are worse than those in Finland and the rest of Scandinavia. **Tuleva** is a member-owned pension cooperative with about 87k savers. It would collect borrowers' loan data and refinancing wishes into one **standardised, risk-classed portfolio**, then run a **joint funding tender** among banks. Later, a partner or a separate fund could refinance the loans, using bonds at scale. Hackathon scope, per the idea sheet: **data pooling, risk classes, and a joint bank offer.**

Related ideas, to stay distinct from:
- **L4**: helps each member compare bank offers individually.
- **P3**: joint procurement of home insurance.

## What we are building

1. **Persuade.** Show mortgage holders what they could gain by joining the pool. **Teammates own this content.** We only render it, through a slot or route in the app.
2. **Collect.** Gather everything needed to include a borrower in the pool, with as little friction as possible. **This is our focus.**
   - Prefer pre-fill from eID and public registers.
   - **Upload documents and extract fields with an LLM** (Claude API). The user confirms every extracted field.
   - Fall back to self-declaring.
3. **Commit.** The flow ends with the borrower **signing a conditional commitment** with Smart-ID / Mobile-ID: "I will switch if the tender beats my margin by ≥ X pp." See PLAN.md, "Commitment model".

**Borrower flow only.** There is no bank-facing pool dashboard or export in the demo. The data model should still be pool-ready, with fields named after the ESMA template. The borrower sees an indicative **risk class**.

Target users: **anyone in Estonia with a mortgage**. Joining the pool requires Tuleva membership (€150, D32); existing members skip it.

The landing page has a Tuleva-style **savings calculator** (worst, expected and best case margin) and a live **pool tracker**: people joined, combined balance, target volume, deadline, and a margin ladder that falls as the pool grows. After document upload, the calculator re-runs on validated numbers.

## Constraints and decisions so far

PLAN.md is the source of truth for decisions (D1…).

- **The repo is PUBLIC.** Never commit:
  - `docs/inputs/` (gitignored; Tuleva-internal material),
  - temporary files in the repo root (gitignored),
  - any real personal or loan data,
  - API keys.

  Don't copy figures from `docs/inputs/` into committed files.

- **Timebox:** 1–2 days of build. The demo **runs locally** and uses **real integrations where feasible**, with mocks behind the same interfaces otherwise.
- **Stack:** match Tuleva so the result could be adopted later.
  - Backend: **Java + Spring Boot**, PostgreSQL with Flyway.
  - Frontend: **React + TypeScript** (Vite, React 19), Bootstrap 5.3 with Tuleva's SCSS, react-hook-form, react-intl, TanStack Query.
  - Reference repos: `github.com/TulevaEE/onboarding-service`, `onboarding-client` and `wordpress-theme` (tuleva.ee). All of them use the `master` branch.
  - **Tuleva has authorised the team to reuse all its resources**: code, website theme, calculators, design and logo. Copy rather than reimplement.
    - Keep the MIT notices from the MIT repos.
    - Add an attribution comment on code taken from `wordpress-theme`.
  - Details: `docs/research/08-tuleva-design-and-code.md`.
- **No real Tuleva or bank mortgage data is available.** Tuleva can provide aggregate saver statistics only. These are private and live in `docs/inputs/`. Bank APIs (PSD2, OpenAPI specs) are a later concern.
- **Uploads are acceptable** for the loan contract, payoff statement, account statements and income proof. API integrations are preferred but optional.
- **Claude API key:** provided later. Put extraction behind an interface and use a stub implementation until then.

## Domain cheat-sheet

The full research is in `docs/research/`. Verify before quoting numbers externally.

- **Market:** €13.1bn housing-loan stock (end-2025), about 192k households with a loan, average balance about €68k.
  - Concentrated: Swedbank 40%, SEB 27%, LHV 14%, Luminor 8%, Coop 6%.
  - **Pitch number: the Estonian spread over Euribor is about 1.55 pp vs Finland's about 0.5 pp (ECB MIR, Aug 2026).**
  - About 40% of "new business" is renegotiation with the borrower's own bank. Actually moving to another bank is rare.
- **Rates:**
  - Almost all mortgages are floating: **6M Euribor + a margin fixed for the life of the loan**. 6M Euribor was ≈2.94% on 2026-10-08.
  - **The margin is the thing a tender negotiates.**
  - New-loan margins are about 1.35–1.6%. Green campaigns go down to 1.35% at SEB and 1.49% at LHV and Coop.
  - Legacy-stock margins are claimed to be 1.85–2.2%. This is unverified.
- **Prudential limits (Eesti Pank):**
  - LTV ≤ 85%, or 90% with an EIS guarantee.
  - DSTI ≤ 50%, stress-tested at max(contract rate, 6%).
  - Term ≤ 30 years.
- **Switching today:**
  1. Get an offer.
  2. Get a new valuation from a level 6/7 appraiser (€150–400).
  3. Give the old bank 3 months' notice. Without it, the penalty is up to 3 months' interest.
  4. Sign at the notary and pay the state fee (~€400–500).

  Bill **864 SE**, which would remove the notary step, is **not in force**.
- **National positive credit register** (KJS, adopted June 2026): lenders get access only from **1 March 2029**, and Tuleva or an intermediary never does.
- **Legal shape** (see `docs/research/05-legal-regulatory.md`):
  - Collecting creditworthiness data and tendering it to banks is very likely regulated **mortgage credit intermediation** (KAVS § 4, § 10). A real product needs an FI licence or a licensed partner.
  - The fund manager, Tuleva Fondid AS, can't operate it.
  - The LTV limit doesn't apply to refinancing up to the existing claim.
  - Early repayment of a variable-rate housing loan costs at most 3 months' interest.
  - The persuasion copy must stay generic and educational (Reklaamiseadus § 29). No concrete credit offers.
- **Data design rules:**
  - Tender on a pseudonymised, aggregated pool.
  - Release identifiable data only to the chosen bank, after the member confirms.
  - Get explicit, granular consent for each recipient.
  - Data should be *verifiable* for a later securitisation (STS Art 20(10)), so prefer documents and registers over self-declaration.
- **Risk-class dimensions:** current LTV, DSTI, income stability, region (Harjumaa vs the rest), energy class, loan size, remaining term and age at maturity, payment history, occupancy.
- **Investor data standard:** the ESMA RMBS loan-level template (Reg. (EU) 2020/1224, Annex II). Name our pool fields after it (RREL*/RREC*) where practical.
- **Data sources in Estonia:**

  | Source | Provides | Status |
  |---|---|---|
  | Smart-ID / Mobile-ID | Identity, isikukood (birth date) | Emulated in the demo (D29) |
  | SK / Zetes eID certificate directories (LDAP) | Name by personal code | Public; used for the demo greeting |
  | In-ADS (address register) | Address autocomplete | |
  | Ehitisregister | Building type, area, year, energy class | |
  | Maa-amet | Price statistics | |
  | e-kinnistusraamat | Mortgage holder and amount (not the balance) | Paid |
  | EMTA via the RIA consent service | Income | |
  | PSD2 AIS | Statements | Needs a licensed provider |
  | EPIS | II-pillar contributions, which reveal gross salary | Tuleva-only; legal basis TBD |

## Repo layout

```
CLAUDE.md          – this file
PLAN.md            – decisions (D1…), journey, scope
TASKS.md           – build backlog (T01…), lanes, milestones, cut line, API sketch
README.md          – setup, run and test commands
backend/           – Spring Boot API (see backend/CLAUDE.md)
frontend/          – React + TS app (see frontend/CLAUDE.md)
docker-compose.yml – local Postgres
.githooks/         – personal-data scanner + pre-commit/pre-push (copied from Tuleva, MIT)
docs/research/     – research notes (English), numbered
docs/inputs/       – PRIVATE raw inputs + private digests (gitignored, never commit)
```

**One-time setup per clone:** `git config core.hooksPath .githooks`. The hooks block personal data and files from `docs/inputs/`. Never bypass them with `--no-verify`.

## Working agreements

- **`TASKS.md` lists open work only.** When a task is finished, delete its entry. Git history and the commit message record the outcome. Partly done tasks keep a short note on what remains.
- **Scope is frozen for the initial build** (PLAN.md, decisions D1–D21). Record any change of scope as a new decision in PLAN.md before implementing it.
- Temporary input files are dropped in the repo root. Digest them into `docs/research/` in English, with a confidence or reliability note, so the originals can be deleted. **If the material is internal or confidential, write the digest to `docs/inputs/` instead.**
- Prefer official sources: Eesti Pank, Finantsinspektsioon, Riigi Teataja, EUR-Lex, AKI, RIA.
- Mark unverified or modelled numbers as such.
- Personal data: collect the minimum, record explicit consent per purpose, and never commit real personal data. Use synthetic test data only.
