# Research: legal and regulatory frame

_Researched 2026-10-09 from the current consolidated texts in Riigi Teataja, plus EUR-Lex, FI, Eesti Pank and AKI. **This is research, not legal advice.** Items marked [uncertain] need FI or counsel to confirm._

## Key takeaways

1. **Collecting creditworthiness data and running a bank tender is very likely regulated mortgage credit intermediation.**
   - KAVS § 4 defines intermediation to include "assisting a consumer in acts preceding a credit agreement".
   - A licence is required under § 10(2), as a *hüpoteekkrediidivahendaja* (mortgage credit intermediary).
   - The tied-agent exemption (krediidiagent, §§ 21, 23) covers **one** creditor only, so it doesn't fit a multi-bank tender.
   - Unlicensed activity is a crime under KarS § 372.
   - FI's letter of 06.07.2026 stresses substance over form.

   **Options:**
   - (a) Get the licence.
   - (b) Partner with a licensed intermediary or bank for the regulated step.
   - (c) Pre-clear the model with FI.

   **For the hackathon this doesn't block a prototype.** It does shape how the pitch is worded and which entity would operate the product.
2. **Tuleva Fondid AS (the fund manager) can't do this itself.** IFS § 307 limits the services a fund manager may offer. A product would sit in the cooperative (Tulundusühistu Tuleva) or a subsidiary.
3. **The LTV limit doesn't apply to refinancing up to the existing claim** (Eesti Pank decree nr 17 § 3(8)). Even borrowers whose LTV is high today can be refinanced.
4. **Early-repayment cost on a variable-rate housing loan is at most 3 months' interest** (VÕS § 411(4)). Fixed-rate periods are different: there the lender may claim "reasonable compensation".
5. **Positive credit register (KJS):** adopted 10.06.2026. **Lenders get access from 1 March 2029** (§ 58(3)), and only credit institutions and creditors can use it. **Tuleva gets no access.** This corrects the "~2027–28" figure in `03-data-and-risk-classes.md`.
6. **Low-friction legal data routes exist:**
   - EMTA income data over X-tee with consent.
   - A free land-register extract for owners of their own property.
   - The VÕS § 411(7) payoff statement from the current bank.

## 1. Credit intermediation (KAVS, MCD 2014/17/EU)

- **KAVS § 4** (what counts as intermediation): mediating credit or pointing to the possibility of a contract, and **assisting with pre-contractual acts**. Point 2 has no fee condition.
- **MCD Art 4(5)** says an intermediary is someone acting "for remuneration" and *not merely introducing*. Recital 74 excludes persons who only incidentally introduce a consumer to a creditor.
- **Probably fine without a licence:**
  - generic education and calculators (KAVS § 7(3): non-personalised recommendations to the public are not advice),
  - a waitlist or statement of interest,
  - referral to a single licensed partner with no per-loan fees.
- **Crosses the line:** collecting creditworthiness data, packaging a risk-classed pool and tendering it. A group discount Tuleva negotiates can count as remuneration.
  - [uncertain] whether a purely non-remunerated cooperative activity falls outside "economic activity".
- **Licensed intermediary duties:**
  - Liability insurance (§ 55(4)).
  - Pass all creditworthiness data to the creditor (§ 50(2)).
  - Verify identity and the information provided (§ 47(2)).
  - Retain files for 3 years (§ 47(5)).
  - Fit-and-proper management, staff knowledge and remuneration rules (§§ 38–44).
  - Disclosure duties and a written intermediation contract (VÕS §§ 417¹–419).

## 2. Later stages: a fund refinances, securitisation, bonds

- **Granting or refinancing loans** requires a KAVS creditor licence (hüpoteekkrediidiandja). The creditor must be an AS or OÜ with at least €50k capital (§§ 5(2), 54(2)).
- **Buying a performing loan pool** is not regulated by KIOS, the Credit Servicers Directive transposition, which covers NPLs only. Consumers keep their defences, and the buyer must notify them (VÕS § 412).
- **Covered bonds (pandikiri):** only a credit institution can issue them. A Tuleva bond route would therefore be **RMBS securitisation** or a partner bank's covered bond.
- **Securitisation Regulation 2017/2402:**
  - 5% risk retention (Art 6).
  - Investor due diligence, which applies to Tuleva's funds (Art 5).
  - **STS Art 20(10) excludes loans underwritten on the premise that information "might not be verified". The app's data must be verifiable.** This argues for documents and register data over self-declaration.
- **Pension funds (IFS):**
  - II and III pillar funds **cannot lend to consumers**.
  - They can buy RMBS or notes: 15% per issuer, and 50% in aggregate for unlisted securities.
  - They can buy units of a mortgage fund, up to 50%.
  - Real-estate exposure is capped at 40% for mandatory funds and 70% for voluntary funds. [uncertain] whether mortgage-backed notes count toward that cap.

## 3. Eesti Pank housing-loan rules

Governor's decree nr 17, current from 16.06.2025. These rules bind credit institutions only.

| Rule | Value |
|---|---|
| LTV | ≤ 85%; ≤ 90% if state-guaranteed. **Not applied to refinancing ≤ the existing claim (§ 3(8))** |
| DSTI | ≤ 50% of net verified income; stress rate = max(contract rate, 6%) |
| Maturity | ≤ 30 years |
| Exception quota | ≤ 15% of quarterly new volume |

## 4. Consumer protection

- **VÕS § 411(7): on request, the lender must state the payoff amount, the compensation and its assumptions on a durable medium.** The app can prompt users to request this and upload it. It is the most authoritative source for the remaining balance.
- **Creditworthiness assessment** (VÕS § 403⁴, KAVS §§ 49–50):
  - The lender must verify the borrower's data, using bank statements where needed.
  - It must tell the borrower if a refusal was automated.
  - FI's responsible-lending handbook (22.10.2024) explains these duties but is not binding.
- **ESIS** (the standardised offer sheet, § 403³) and a draft contract must come before a binding offer. This is the bank's or intermediary's duty, not ours in the prototype.

## 5. GDPR and personal data

- **Legal basis:**
  - Art 6(1)(b), pre-contract steps at the person's request, covers their own application.
  - Use **explicit, granular consent** (Art 6(1)(a)) for sharing with named banks. Each bank becomes an independent controller.
- **Bank transaction data can reveal special-category data** (EDPB Guidelines 06/2020). Minimise what we extract from statements and don't store raw transactions longer than needed.
- **Art 22:** automated risk-classing with significant effects needs safeguards. Keep a human in the loop, or make the classing purely informational.
- **Design pattern:**
  1. Tender on a **pseudonymised, aggregated pool** (bands, region, risk class).
  2. Release identifiable data only to the chosen bank, after the member confirms.
- **Isikukood:** fine to use as an identifier. **Never use it to mine registers.** In 2024, AKI reprimanded a person for about 189k land-register queries by isikukood.
- **Using pension data for a credit purpose is a new purpose** (Art 6(4)) and needs separate consent.
- **Negative registry:** Creditinfo's maksehäireregister. Defaults can be shared if they are 30 days to 5 years old (IKS § 10).

## 6. Data sources (legal and access view)

| Source | Access | Notes |
|---|---|---|
| Smart-ID / Mobile-ID / ID-card | SK ID Solutions directly, or Dokobit / eID Easy / Veriff | Qualified e-signature available. SK has a free demo environment |
| PSD2 AIS | Needs an FI licence, **or use a licensed provider** (Enable Banking, GoCardless, Tink, Salt Edge) | Covers **payment accounts only, not mortgage accounts**. Check Swedbank coverage for each vendor |
| EMTA income | (1) The user downloads a digitally stamped *maksuandmete tõend* from e-MTA and uploads it; (2) a consent-based X-tee service via RIA's consent service | Option 2 needs X-tee membership plus EMTA approval, **so it is not feasible in the hackathon**. Option 1 is |
| Land register (KRS) | Anyone can view by registry number, cadastral number or address. Up to €3 per query; **free for owners viewing their own property** | Searching by isikukood is restricted to banks and notaries |
| Building register (EHR) | Public open data: area, year, energy certificates | [unverified in this session] |
| Maa- ja Ruumiamet prices | Public statistics | [API terms unverified] |
| Loan details | Statement upload, the § 411(7) payoff statement, or a GDPR Art 15 request | FiDA would cover this in future; its status is uncertain |

## 7. Marketing rules (Reklaamiseadus § 29)

- Every financial-service advert must invite people to read the terms (§ 29(2)).
- **Adverts offering to grant or intermediate housing credit** must:
  - include the licence number and a representative example (rate, APR, total cost);
  - state that the loan is secured by a mortgage;
  - show the APR at least as prominently as the rate.
- **Adverts must be balanced** (§ 29(7)).
- **Implication for the "persuade" part:**
  - Keep messaging **generic and educational**, e.g. "pooling could lower margins; here's what a 0.5 pp cut is worth on your loan".
  - Make no concrete credit offer until a licensed party exists.
  - The hackathon brief also states that challenges "are not offers of Tuleva products or services".

## Sources

- **Laws (Riigi Teataja):**
  - KAVS: https://www.riigiteataja.ee/akt/120062026024
  - VÕS: https://www.riigiteataja.ee/akt/120062026018
  - KJS: https://www.riigiteataja.ee/akt/120062026004
  - KIOS: https://www.riigiteataja.ee/akt/120062026014
  - Pandikirjaseadus: https://www.riigiteataja.ee/akt/113022026012
  - IFS: https://www.riigiteataja.ee/akt/111072026070
  - Eesti Pank decree nr 17: https://www.riigiteataja.ee/akt/113062025028
  - KRS: https://www.riigiteataja.ee/akt/128112024005
  - Reklaamiseadus: https://www.riigiteataja.ee/akt/106032026008
  - IKS: https://www.riigiteataja.ee/akt/130062026052
- **EU law:**
  - MCD: https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32014L0017
  - Securitisation Regulation: https://eur-lex.europa.eu/eli/reg/2017/2402/oj
- **Regulators:**
  - FI letter 06.07.2026: https://www.fi.ee/sites/default/files/2026-07/06.07.2026_nr_4.11-3.1-4709.pdf
  - FI intermediary licence: https://www.fi.ee/et/pangandus-ja-krediit/krediidivaldkonna-tegevuslubade-taotlemine/krediidivahendaja-tegevusluba
  - FI responsible-lending handbook: https://www.fi.ee/et/juhendid/pangandus-ja-krediit/finantsinspektsiooni-kasiraamat-vastutustundliku-krediidi-valjastamise-protsessile
  - EDPB Guidelines 06/2020: https://www.edpb.europa.eu/our-work-tools/our-documents/guidelines/guidelines-062020-interplay-second-payment-services_en
  - AKI reprimand: https://www.aki.ee/sites/default/files/documents/2024-10/Noomitus_e-kinnistusraamat_1.pdf
- **EMTA:**
  - Tax data certificate: https://emta.ee/eraklient/e-teenused-maksutarkus/registrid-paringud/toendite-taotlemine
  - X-tee service: https://emta.ee/form/taotlus-fuusilise-isiku-maksuand
- **Open banking:**
  - Enable Banking (Estonia): https://enablebanking.com/docs/markets/ee/
  - Aggregator list: https://openbankingtracker.com/country/ee/aggregators
- **Tip:** fetch full act text from `https://www.riigiteataja.ee/public-api/api/v1/akt/{id}/txt`.
