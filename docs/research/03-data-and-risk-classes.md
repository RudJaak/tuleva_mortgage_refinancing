# Research: what data to collect and how risk is segmented

_Researched 2026-10-09. Sources are listed at the bottom. Figures come from bank pages that change often, so re-check them before quoting._

## 1. How refinancing works in Estonia today

The flow is the same at all banks:

1. The borrower applies. A preliminary offer arrives within minutes or within 24 hours.
2. The borrower sends documents, and the bank makes a binding offer.
3. The borrower gives the old bank notice. With 3 months' notice there is usually no early-repayment fee; otherwise the fee is at most 3 months' interest.
4. The property gets a new valuation.
5. At the notary, the mortgage moves from the old bank to the new one, and the new bank pays off the old loan.

**Documents banks consistently require:**
- An ID document.
- **Statements for all accounts covering the last 6 months.** These serve as proof of both income and payment history.
- An **expert valuation**:
  - No older than 3–12 months.
  - Must be signed by a level 6 or 7 certified appraiser (Minister of Finance regulation no. 25, 2016).
  - Costs about €150–400. LHV's €150 figure was later found only for *new* apartments (see `06`); a typical full report costs €220–460.
- Home insurance with the bank named as beneficiary.
- Optional: EIS (formerly KredEx) guarantee papers.

No bank publicly asks for the **old loan contract**. The payoff amount is settled between the banks at closing.

**Prudential limits** apply to every new loan, refinancing included (Eesti Pank):
- **LTV ≤ 85%**, or 90% with an EIS guarantee.
- **DSTI ≤ 50%**, stress-tested at max(contract rate, 6%).
- **Term ≤ 30 years.**

**Switching cost** is up to about €3,000 on a €100k loan (Eesti Pank estimate). It is made up of:
- the early-repayment fee,
- the new bank's arrangement fee (0.4–0.9%, minimum €100–300),
- the valuation,
- the notary and state fees.

Bill **864 SE** would remove the notary requirement when only the lender changes. It has passed first reading only and is **not in force**.

**National credit register (KJS):** adopted June 2026. **Lender access starts 1 March 2029**, and only credit institutions and creditors get it, so Tuleva never does. See `05-legal-regulatory.md`.

### Bank snapshot (2026)

| Bank | Refinancing offer | Price signals |
|---|---|---|
| SEB | Campaign code REFIN26 for 2026. SEB pays notary fees on loans over €100k. | From 1.35% + Euribor for A/B energy-class homes. Fee 0.4%, minimum €190. |
| LHV | Dedicated refinancing flow with an offer within 24h. Can merge loans. | Green loans from 1.49% + 6M Euribor. Fee 0.5–0.9%, minimum €300. |
| Coop Pank | Standard transfer, plus a campaign for teachers. | From 1.49% on 6M Euribor for energy class A. Fee €100. |
| Bigbank | "Refinance on better terms." | 3/6/12M Euribor. Fee 0.4%, minimum €150. Minimum net income €600/month. |
| Swedbank | No refinancing page found. Automatic preliminary offer. | No 2026 figures found. |
| Luminor | Not checked (page did not render). | — |
| Inbank | No consumer mortgage product. | — |

## 2. Euribor

As of 2026-10-08:

| Tenor | Rate |
|---|---|
| 1M | 2.414% |
| 3M | 2.642% |
| **6M** | **2.936%** |
| 12M | 3.213% |

**6M Euribor is the Estonian standard.** Most loans have a variable rate with the **margin fixed for the life of the loan**, so the margin is what a joint tender can actually negotiate. Typical current margins are about 1.35–1.8%.

## 3. Risk-class dimensions

These buckets are a synthesis of market practice, not a published standard. The reference template is the **ESMA RMBS loan-level template** (Delegated Reg. (EU) 2020/1224, Annex II), which is what investors and European DataWarehouse expect.

| # | Dimension | Suggested buckets | ESMA field |
|---|---|---|---|
| 1 | Current LTV (balance ÷ fresh valuation) | ≤50 · 50–60 · 60–70 · 70–80 · 80–85 · >85 (EIS) | RREC12 |
| 2 | DSTI at stress rate | <30 · 30–40 · 40–50 | RREL40 |
| 3 | Income stability | permanent long-tenure · fixed-term · self-employed (FIE) · dividend/foreign; verified vs declared | RREL13/17/19 |
| 4 | Region | Tallinn + Harjumaa · Tartu · other cities · rural | RREL11 / RREC6 |
| 5 | Energy class | A/B (green) · C/D · E–H/unknown | RREC10 |
| 6 | Loan size | <€50k · €50–150k · >€150k | RREL30 |
| 7 | Remaining term / age at maturity | flag age >70 at maturity | RREL24 |
| 8 | Payment history | no arrears in last 12–24 months (gate) | RREL66–69 |
| 9 | Seasoning | months since origination | RREL23 |
| 10 | Occupancy | owner-occupied vs rental | RREC7 |

## 4. UX precedents for frictionless data collection

- **Sweden (SEB with Kreditz):** the applicant logs in with BankID to the tax authority (Skatteverket) and to their salary bank. The data is passed on and then deleted. If that fails, the applicant uploads 3 payslips.
- **Finland:** lenders query the Positive Credit Register, which gives 12 months of income plus all of the borrower's loans.
- **Sweden (Stabelo / Avanza Bolån+, Hypoteket):** "move your mortgage" products funded by institutional investors. They cap LTV at about 60–65% and apply via BankID.
- **Estonian equivalents:**
  - Smart-ID, Mobile-ID or ID-card login.
  - The **RIA consent service (nõusolekuteenus)** with EMTA taxpayer data over X-tee. The Data Protection Inspectorate (AKI) approved the consent flow in November 2024.
  - PSD2 account-information access through a licensed provider.
  - The e-land register (kinnistusraamat): who holds the mortgage and its amount, but **not the outstanding balance**.
  - The building register (Ehitisregister): building type, area, year built, energy class.
  - Maa-amet transaction statistics, for an indicative valuation (AVM).

## 5. Proposed minimal data schema

| Field | Why | Auto-source in Estonia | Req |
|---|---|---|---|
| Isikukood, name | Identity, KYC, age | Smart-ID / Mobile-ID / ID-card | Req |
| Birth date / age | Age at maturity | Derived from isikukood | auto |
| Address, residency | Eligibility | Population register | auto |
| Co-borrower(s) | Joint income and liabilities | Second eID login via an invite | Opt |
| Household size, dependants | Living-cost buffer (EBA loan-origination guidelines) | Self-declared | Req |
| Net monthly income | DSTI | PSD2 statement; EMTA via the consent service | Req |
| Gross employment income | Verification | EMTA; II-pillar contributions (2/4/6% of gross) via EPIS, **legal basis unverified** | auto pref. |
| Employment type, tenure, employer | Income stability | EMTA employment register; self-declared | Req |
| Other loans and leases | Total DSTI | Statement debits (the credit register is not accessible to Tuleva) | Req |
| Current lender | Tender, payoff | Self-declared; land register mortgage holder | Req |
| Remaining balance | LTV, pool size | Self-declared; screenshot or statement upload | Req |
| Margin + Euribor tenor | Savings estimate, benchmark | Self-declared; contract upload; derived from statement | Req ("unknown" allowed) |
| Monthly payment | DSTI, balance sanity check | Statement | auto |
| Origination date, maturity | Seasoning, term | Self-declared / contract | Req |
| Fixed-rate period / prepayment terms | Switching cost | Contract | Opt |
| Arrears in last 12–24 months | Performance gate | Statement; Creditinfo payment-default register | auto |
| Property address, registry part / cadastral no. | Collateral identity | Address register (ADS) + e-land register | Req |
| Property type, area, year built | Collateral class | Ehitisregister | auto |
| Energy class | Green tier | Ehitisregister energy certificate | Opt/auto |
| Owner-occupied vs rental | Occupancy | Self-declared | Req |
| Indicative value | Indicative LTV | Maa-amet statistics (AVM); self-estimate | Req (indicative) |
| Expert valuation | Binding LTV | Upload | Stage 2 |
| Lien rank / other mortgages | Recovery | e-land register | auto |
| EIS guarantee | Special scheme | Self-declared | Opt |
| Refinancing wish (keep term / extend / cash-out / merge) | Purpose | Self-declared | Req |
| Consents (share with banks, credit check) | GDPR, tender | Signed with Smart-ID | Req |

**Suggested 60-second first step:**
1. Log in with Smart-ID.
2. Which bank holds your loan?
3. Roughly how much is left to repay?
4. What is your current margin? ("I don't know" is allowed.)
5. Property address, autocompleted.
6. Consents.

Everything else is either pre-filled or deferred to stage 2. A binding valuation is requested only once a bank has shown real interest.

## Sources

- Banks:
  - Coop: https://www.cooppank.ee/kodulaen ; https://cooppank.ee/ariklient/blogi/mida-peaksin-teadma-kui-soovin-kodulaenu-uude-panka-ule-tuua
  - LHV: https://www.lhv.ee/et/kodulaen
  - SEB: https://www.seb.ee/eraklient/laenud/kodulaen
  - Bigbank: https://www.bigbank.ee/kodulaen/
  - Swedbank: https://www.swedbank.ee/private/home/loans/home
- Eesti Pank:
  - Limits and stress rate: https://www.err.ee/1609234017/eesti-pank-muudab-pankade-reegleid-eluasemelaenude-valjastamisel
  - Switching costs: https://www.err.ee/1609266390/eesti-pank-kodulaenu-voiks-saada-lihtsamalt-teise-panka-ule-viia
- Bill 864 SE: https://www.riigikogu.ee/tegevus/eelnoud/eelnou/9035812d-3c51-43e0-b564-d371ce6c7876/
- Credit register: https://news.err.ee/1610052295/estonian-lawmakers-approve-new-nationwide-loan-register
- Appraiser levels: https://www.err.ee/585290/nuudsest-voivad-kinnisvara-hinnata-vaid-kutselised-hindajad
- ESMA RMBS template (Reg. 2020/1224 Annex II): https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32020R1224
- EBA NPL templates: https://www.eba.europa.eu/documents/10180/2347128/212a105e-54c9-4230-8b25-90f20e022c97/EBA%20-%20NPL%20Transaction%20Template%20Instructions_final.pdf
- Euribor: https://www.euribor-rates.eu/en/current-euribor-rates/
- Estonian consent service: https://www.err.ee/1608414620/uus-nousolekuteenus-voimaldaks-riigi-kogutud-andmeid-jagada-ettevotetega
- SEB Sweden / Kreditz: https://seb.se/privat/bolan/lanelofte/ansok-om-bolanelofte/bekrafta-dina-inkomstuppgifter
- Finland Positive Credit Register: https://vero.fi/en/positivecreditregister/information-about-the-register/our-data-protection-policy/income-data
