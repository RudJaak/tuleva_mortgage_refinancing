# Research: what banks ask home-loan applicants, and what our flow leaves unanswered

_Researched 2026-10-10. Bank sites change often and several block scraping. LHV is the only bank whose full application form was readable; the others are based on product pages, calculators and press. Legal paragraph numbers marked "(verify)" are from memory and were not re-read in the consolidated text._

## Short answer

- **Our flow already covers the hard, verifiable core:** identity and age, the current loan (from the contract), and income, debt service and payment behaviour (from 6 months of statements). That is most of what a bank needs for a **preliminary** decision on DSTI, LTV and term.
- **The gaps are mostly self-declared questions that banks ask on every form**, and that a bank cannot infer from statements:
  1. **Number of dependants / household size.** Every bank uses it for living-cost assumptions (LHV form, Citadele and Swedbank calculators, SEB's affordability text).
  2. **Employment type and time at current job**, incl. probation, fixed-term, FIE (sole trader) and dividend income. LHV asks both directly.
  3. **Liabilities that do not show up as payments:** guarantees given (käendused), and unused credit-card / overdraft **limits**. LHV asks for both.
  4. **Foreseeable income changes** (parental leave, contract end, retirement). EU creditworthiness rules require lenders to consider them.
  5. **What the borrower wants from the refinancing**: same balance, or extra cash (cash-out). LHV asks "Soovin täiendavat laenusummat".
- **KYC/AML items (PEP status, tax residency, citizenship / residence permit, source of funds) are real bank questions, but each bank redoes KYC on its own at the binding stage.** They do not change the pooled price. Collect them later, from the chosen bank.
- **Property items (current value, energy class, insurance) matter for pricing** but should come from registers and the valuation, not from self-declaration. Leave them for later (EHR, Maa-amet price index, valuation).
- **Recommendation:** add **five short self-declared questions** (dependants; employment type; time at current job; guarantees and credit limits; expected income change), plus **one confirmation** that statements cover all accounts. Optionally ask about cash-out. Wording is below.

## Regulatory frame (why banks ask)

| Rule | What it requires | Effect on questions |
|---|---|---|
| **Eesti Pank Governor's Decree no. 17 (12.12.2014)**, as amended (latest 16.06.2025) | LTV ≤ 85% (90% with state guarantee); DSTI ≤ 50% of **net income**, counting **all** the borrower's credit; stress rate max(contract rate, 6%); term ≤ 30 y; 15% of new loans per quarter may breach | Needs all liabilities (incl. other banks), net income, collateral value. Eesti Pank says income must above all be **regular**; which income types count is the bank's call (ERR, Oct 2025) |
| **VÕS § 403⁴ and KAVS §§ 49–50** (creditworthiness), transposing MCD 2014/17/EU Art 18 | Thorough creditworthiness assessment on necessary, sufficient, proportionate info on **income, expenses and other financial circumstances**, appropriately verified; consider foreseeable future events | Basis for employment, income stability, dependants / expenses, and "expected changes" questions |
| **EBA Guidelines on loan origination and monitoring (EBA/GL/2020/06)**, applied in Estonia by Finantsinspektsioon | Section 5.2 and Annex 2 list info to collect from consumers: purpose, employment status and stability, income, **household composition and dependants**, living expenses, financial commitments incl. **contingent ones (guarantees)**, assets | FI withdrew its own "responsible lending" recommendation in 2021 in favour of these. Detailed annex list quoted from memory (verify) |
| **RahaPTS** (AML/KYC): due diligence (§ 20, verify), enhanced measures, PEPs (§ 41, verify); EU AMLR 2024/1624 applies from July 2027 | Identity, purpose of the relationship, PEP status, source of funds / wealth where risk warrants, tax residency (CRS / FATCA) | Done by the **lending bank itself**; cannot be outsourced to a pool collector in a way that the bank can rely on without a reliance agreement |
| **PKS** (Family Law Act) | Spouse consent to encumber the family home or joint property | Marital status: notary stage (D27) |

## What each bank publicly asks

| Bank | Source read | Asked beyond what we collect |
|---|---|---|
| **LHV** | Full online form (`/et/kodulaen/taotlus`) | Purpose ("Refinantseerimine") and **reason for moving**; **extra amount** wanted; estimated market value; residency + ID-issuing country; **marital status**; **dependants (0–6+)**; **education**; **occupation category** (entrepreneur, FIE, manager, specialist, skilled / service worker, student, pensioner, unemployed); job title; **employer** and **employer's industry**; **time at current job (probation, <1, <2, <5, >5 y)**; incomes by type (net salary, rent, dividends, pension, child / parental / social benefits, other); liabilities by type incl. **guarantee** (balance, payment, end date, lender); **monthly household expenses**; **credit limits (card, overdraft)**; consents incl. automated decision |
| **Swedbank** | Product page | Proven net income ≥ €1,000 (6-month average; €1,300 joint; €1,500 with dependants, page is inconsistent); all payments incl. credit card and hire purchase ≤ 50%; dividends count for owners, with company documents. Does **not** count family benefits since Sept 2024 |
| **SEB** | Product page | Payment limit depends on income, **number of household members** and existing commitments; insurance at rebuild value; SEB current account |
| **Citadele** | Refinancing page / calculator | Net income, **number of applicants, number of dependants**, monthly obligations and their balance |
| **Coop Pank** | FAQ | Income types (salary, business, rent, benefits, pension); **regularity and sustainability**; foreign income if documented; purpose (refinancing); existing commitments |
| **Bigbank** | Product page | Regular, official, **euro-denominated** income ≥ €600 (€950 joint); **Estonian registered address**; **citizenship or valid residence permit**; age ≥ 18 |
| **Luminor** | Not readable | Unknown (snippets only, see `06`) |
| **Inbank** | – | No home-loan product found (see `03`) |

## Gap table

"Verified by us" means our current flow answers it from a document or register. Everything below is **not** answered with verified data today.

| # | Question | Why banks ask | Verifiable how | Add to our flow? |
|---|---|---|---|---|
| 1 | **Number of dependants / household size** | Living-cost deduction in affordability (EBA GL household composition; bank internal norms). Swedbank raises the income floor with dependants; LHV, Citadele and SEB use it. Also drives EIS large-family guarantee eligibility | Self-declared; later the population register (eesti.ee children extract) | **Yes.** Cheap, asked by every bank, changes the risk class. Statements cannot show it |
| 2 | **Employment type** (permanent / fixed-term / probation / FIE / board member with dividends / pensioner / other) | Income stability; MCD "foreseeable events"; FIE and dividend income need tax return or annual report | Self-declared now. Later: the **employment register (TÖR)** extract on eesti.ee shows employer, start date and contract type (verify which fields); EMTA tax-data certificate (D20 "could") | **Yes.** Core income-stability dimension in our risk class (`03`, dimension 3), which statements show only indirectly |
| 3 | **Time at current employer** | Stability; banks are wary of probation | Self-declared; TÖR start date; statements partly (first salary from a new payer) | **Yes**, same question block as 2 |
| 4 | **Guarantees given (käendused)** and co-signed loans | Contingent liability; EBA GL; LHV lists "käendus" as a liability type | Self-declared. No register for us before KJS (lenders only, 2029) | **Yes.** Invisible in statements, as no payments flow |
| 5 | **Credit-card and overdraft limits** | Banks often count a share of the unused limit as debt service (practice varies, unverified); LHV asks for limits | Self-declared; the bank sees its own limits; partly from statements (card repayments) | **Yes**, folded into the question 4 block. One sum field is enough |
| 6 | **Loans paid from accounts not uploaded** (other banks, a partner's account) | DSTI must include all credit | Our D28 rule "statements for every account" covers it; only an explicit confirmation is missing | **Yes**, as a checkbox, not a question |
| 7 | **Expected income change in 12 months** (parental leave, contract end, retirement, moving abroad) | MCD Art 18 / EBA GL: consider foreseeable future events. Parental benefit (vanemahüvitis) is temporary income | Self-declared only | **Yes.** A "yes" lets the pool handle the case honestly instead of the bank finding it late |
| 8 | **Refinancing purpose: same balance, extra cash, consolidate other loans, change term** | Cash-out raises LTV and changes product; consolidation changes DSTI | Self-declared | **Optional now.** Useful to keep the pool homogeneous ("pure switch" tranche); otherwise later |
| 9 | Employer name and **sector** | Cyclical-sector risk (LHV asks industry) | Employer name already in statements (salary payer); sector from the business register EMTAK code | **No new question.** Derive later from the payer name |
| 10 | **Education** | Statistical scoring input (LHV) | Self-declared | **No.** Weak, intrusive, not needed for pooled pricing |
| 11 | **Monthly household expenses** | Affordability (LHV asks) | Self-declared; statements could estimate it but we keep only aggregates (D26) | **No.** Banks apply their own expense norms based on household size (question 1) |
| 12 | **PEP status** (self, family, close associates) | RahaPTS enhanced due diligence | Self-declared + bank screening lists | **Later.** The bank does it at onboarding; it does not change pool pricing |
| 13 | **Tax residency** (CRS/FATCA), **citizenship / residence permit**, registered address in Estonia | AML/KYC; Bigbank requires residence permit for foreigners | Personal code and eID show an Estonian code, not residence status; population register | **Later**, at KYC. An Estonian personal code is a good enough proxy for the pool |
| 14 | **Source of funds / wealth** | AML; for purchases, the down payment's origin (LHV) | Documents | **Later / no.** No down payment in a pure refinancing |
| 15 | **Marital status, property regime, spouse consent** | PKS family home rule | Population register; notary | **Later** (D27, notary stage) |
| 16 | **Current property value** | LTV at decision date | Expert valuation (or AVM from 2027); Maa-amet price statistics for an index | **Later.** Index the signing value with Maa-amet statistics; self-declared values are optimistic |
| 17 | **Property type and energy class** | Collateral; green pricing (SEB REFIN26 A/B; LHV / Coop green margins) | EHR (building register) | **Later via EHR** (dropped from the demo by D28). High value to banks for green tranches |
| 18 | **Home insurance** | Mandatory collateral cover | Policy | **Later**, contract stage |
| 19 | **Income sources not in euro / abroad** | Bigbank: euro income only; Coop: foreign income if documented | Statements show it if the account is uploaded | **No new question**; covered by statements + question 2 |

## Does adding them make the pool more attractive?

- **Yes for 1–7.** They close the gap between our "preliminary-grade" data and the bank's own first-screen form. A bank pricing a pool cares most about **DSTI after living costs** (needs 1), **income durability** (needs 2, 3, 7) and **hidden debt** (needs 4–6). Without these a bank has to assume the worst for the whole pool or re-ask everyone, which weakens the tender.
- **Self-declared is acceptable at the tender stage.** Banks bid on a pseudonymised pool and then run their own checks on each borrower. What makes the pool bankable is that **fields are declared consistently and tagged by source** (declared vs document-verified, as in PLAN.md).
- **KYC/AML items do not improve the price.** They are mandatory per bank and per relationship, and a bank will re-collect them anyway.

## Recommended additions now (one short screen or part of "Väljavõtted")

All self-declared, each tagged "declared". Estonian wording suggestions:

1. **Dependants**
   - "Mitu ülalpeetavat sul on?" (0 / 1 / 2 / 3 / 4+)
   - Hint: "Lapsed või teised pereliikmed, keda sa rahaliselt ülal pead."
2. **Employment type**
   - "Mis on sinu peamise sissetuleku allikas?"
   - Options: "Tähtajatu tööleping" · "Tähtajaline tööleping" · "FIE" · "Ettevõtte omanik (palk ja/või dividendid)" · "Pension" · "Muu"
3. **Time at current job** (shown for employment options)
   - "Kui kaua oled praeguse tööandja juures töötanud?"
   - Options: "Olen katseajal" · "Alla 1 aasta" · "1–2 aastat" · "2–5 aastat" · "Üle 5 aasta"
4. **Off-statement liabilities**
   - "Kas oled kellegi laenule käendaja või kaaslaenaja?" (Jah / Ei; if yes: "Kohustuse jääk, €")
   - "Sinu krediitkaartide ja arvelduskrediitide limiitide summa kokku, €" (0 allowed)
5. **Expected income change**
   - "Kas eeldad, et su sissetulek muutub järgmise 12 kuu jooksul oluliselt?"
   - Options: "Ei" · "Jah, väheneb (nt lapsehoolduspuhkus, lepingu lõpp, pension)" · "Jah, suureneb"
6. **Confirmation** (checkbox, not a question)
   - "Kinnitan, et lisasin väljavõtted kõigist oma pangakontodest ja et mul ei ole teisi laene ega liisinguid, mida nendel väljavõtetel ei näe."
7. _Optional:_ **Purpose**
   - "Mida soovid laenu ümberviimisel?"
   - Options: "Ainult paremat marginaali" · "Lisaks laenusummat suurendada" · "Muuta laenu tähtaega"

**Not now:** education, household expenses, PEP, tax residency, citizenship, source of funds, marital status, insurance (later, at bank / notary). Energy class and property value come from EHR and a price index or valuation later.

## Confidence

- **High:** LHV form fields (form read directly); Eesti Pank limits and the stress rate (Eesti Pank page); Swedbank income floors and the benefits change; Bigbank residency / income rules.
- **Medium:** SEB and Citadele (product pages and calculators only, no forms); the claim that "every bank" asks about dependants (seen in LHV, Citadele, SEB, Swedbank; not seen for Coop, Luminor, Bigbank).
- **Low / unverified:**
  - Exact RahaPTS § numbers and the EBA GL Annex 2 wording (from memory).
  - How banks treat unused credit limits in DSTI.
  - Which fields the TÖR extract on eesti.ee exposes (contract type in particular).
  - Luminor's form (not readable).
  - The June 2025 amendment to Decree no. 17 was not checked for content.

## Sources

**Banks**
- LHV application form: https://www.lhv.ee/et/kodulaen/taotlus
- Swedbank home loan: https://www.swedbank.ee/private/credit/loans/home?language=EST
- SEB home loan: https://www.seb.ee/eraklient/laenud/kodulaen
- Citadele refinancing: https://www.citadele.ee/et/private/mortgage-refinance/
- Coop Pank FAQ: https://www.cooppank.ee/en/info/faq/home-loan
- Bigbank home loan: https://www.bigbank.ee/kodulaen/
- ERR, Swedbank stops counting family benefits (Oct 2025): https://www.err.ee/1609829013/swedbank-ei-arvesta-enam-peretoetusi-kodulaenuvotja-sissetulekute-hulka

**Regulation and guidance**
- Eesti Pank, requirements for housing loans: https://www.eestipank.ee/en/financial-stability/requirements-housing-loans
- Eesti Pank, 2024 amendment (stress rate): https://www.eestipank.ee/en/press/eesti-pank-amending-requirements-banks-regulating-issuing-new-housing-loans-26012024
- Decree no. 17, consolidated (EN): https://www.riigiteataja.ee/en/eli/516102025002/consolide
- KAVS: https://www.riigiteataja.ee/akt/120062026024 (see `05-legal-regulatory.md`)
- EBA Guidelines on loan origination and monitoring (EBA/GL/2020/06): https://www.eba.europa.eu/regulation-and-policy/credit-risk/guidelines-on-loan-origination-and-monitoring
- Finantsinspektsioon responsible-lending manual (draft and consultation responses, status unconfirmed): https://adr-docs.karlerss.com/PTAxN3iP53eaQ7naVGfoxfcQ3pw8Jywj/Finantsinspektsiooni%20k%C3%A4siraamat%20vastutustundliku%20krediidi%20v%C3%A4ljastamise%20protsessile.docx (unofficial mirror of a public-consultation document)
- RahaPTS (AML act): https://www.riigiteataja.ee (search "rahapesu ja terrorismi rahastamise tõkestamise seadus")
- Related notes: `03-data-and-risk-classes.md`, `05-legal-regulatory.md`, `06-refinancing-documents.md`
