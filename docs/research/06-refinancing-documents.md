# Research: documents Estonian banks require to refinance a home loan

_Researched 2026-10-10. Several bank sites block scraping, so some findings come from search-engine snippets of official pages. Those are marked "(snippet)"._

## Bottom line

- **No bank publishes a separate refinancing document list.** Refinancing is handled as a normal housing-loan application, with the amount set to the current balance and a down payment of €0.
- **Up front:** ID, **6 months of statements for all accounts, sealed by the issuing bank** (.asice / .bdoc / .sce), and declared income and liabilities.
- **Before signing the contract:** an **expert valuation** from the bank's list of accepted firms. **The borrower orders and pays for it.**
- **At the contract and notary stage:** insurance with the bank as beneficiary, the spouse's signature if the property is the family home or joint property, and an early-repayment application to the old bank.
- **No bank asks for the old loan contract.** LHV's form asks for the old contract **number**, the creditor and the balance.

### What this means for our app

1. **Banks expect bank-sealed statements, not plain PDFs.** LHV wants a sealed .bdoc and Coop wants statements digitally sealed by the issuing bank. **That makes the ASiC-E/BDOC sealed upload the "bank-grade" channel, not a nice-to-have** (see `07-account-data-access.md`). A plain PDF is fine for our indicative risk class but probably not for the bank.
2. **Statements go stale.** Coop wants them up to the day before the application. We need a **refresh just before the tender or submission**. A 180-day AIS consent (Enable Banking) solves this neatly; with uploads, the user re-uploads.
3. **The valuation is the costly item (≈ €220–460) and is only needed before signing.** Don't ask for it up front. Optionally, offer to order it after the tender from a firm accepted by all banks.
4. **Family-law status matters at the notary.** Collect marital status and the property regime up front so there are no surprises later.

## Legal frame

- **AÕS § 338(2), since 1.2.2023:** transferring a mortgage needs only an agreement between the old and new bank, notarised **or digitally signed**, plus a land-register entry.
- **AÕS § 346(2):** amending the secured claims still needs a notary. That is why a notary is involved today.
- **Bill 864 SE** would remove this when only the lender changes and terms don't get worse.
  - It is still **not adopted**: first reading only, amendment deadline 6.5.2026.
  - The Riigikogu legal affairs committee has expressed support.
- **Cost example from the bill's explanatory memo** (€150k loan):

  | Item | Cost |
  |---|---|
  | Notary | €275 |
  | State fee | €75 |
  | Valuation | ≈ €280 |
  | Contract fee | ≈ €1,500 |
  | 3 months' interest | €1,683 |

- **Valuation regulation change (Ministry of Finance, 4.3.2026):** banks may use an **automated valuation (AVM)** instead of an on-site appraisal "where data is sufficient". Banks must comply by early 2027. The appraisers' association (EKHÜ) objects. **No bank offers it publicly for refinancing yet.**
- **Credit register (KJS):** in force 30.6.2026. Lenders start reporting on 1.7.2028; mandatory queries start 1.3.2029.
- **Family Law Act (PKS):** encumbering the **family home** needs the spouse's consent, whoever owns it. Joint marital property needs both spouses. LHV: "the spouse must come and sign the notarial collateral agreement". The exact § subsection is unverified.

## Master checklist

✓ = publicly required · ? = not found · Sw = Swedbank, Lum = Luminor, Big = Bigbank, Cit = Citadele

| Document | Purpose | Banks | Stage | Who obtains | Source / format | Collect up front? |
|---|---|---|---|---|---|---|
| ID / identification | KYC | Coop ✓, Lum ✓ (snippet), LHV ✓ (at signing), Holm ✓ (Veriff) | prelim / contract | borrower | ID card, passport; Smart-ID | partly; each bank redoes KYC |
| **Account statements, all accounts, 6 months** | income, spending, payment history | LHV ✓, Coop ✓, Lum ✓ (snippet), Cit ✓; Big "4 months" on one page; Sw/SEB ? | prelim | borrower | Internet bank. **Bank-sealed .asice/.bdoc/.sce.** Statements from other banks: PDF signed with DigiDoc | **yes**, with a refresh before submission |
| EMTA tax-data certificate (maksuandmete tõend) | verified income | LHV and Coop query EMTA with consent (confirmed for consumer loans; home loans unconfirmed) | prelim | bank with consent, or borrower downloads it | e-MTA → Registrid ja päringud → Tõendid; DigiDoc with EMTA digital stamp | **yes**; especially for FIE (sole trader) and dividend income |
| FIE / dividend / rental / foreign income proof | income | Coop ✓ (foreign income if "regular, sustainable, documented") | prelim | borrower | Tax return incl. annex E, annual report, lease contracts | partly; collect the last tax return + 12 months of evidence |
| Liabilities (loans, leases, guarantees) | DSTI | all ✓ | prelim | borrower | Application form | yes. Coop: **don't** list the loan being refinanced |
| Old loan: creditor, balance, **contract number** | deal size, payoff | LHV ✓ (form fields), Lum ✓, Sw ✓, Coop ✓ | prelim | borrower | Old bank's internet bank, "Laenud ja liising" (Loans and leasing) | **yes** |
| Old loan **contract** | – | none require it | – | – | – | **optional, but useful to us**: margin, fixed period, early-repayment terms, mortgage amount |
| Payment schedule (maksegraafik); payoff statement (VÕS § 411(7)) | exact payoff | not required as an upload | binding / notary | borrower requests; payoff is settled between banks | Internet bank; Luminor issues certificates on request | schedule yes; final payoff no (needs a notary date) |
| **Early-repayment application to the old bank** | triggers payoff | Lum ✓ (≥ 10 working days before notary), Coop ✓, LHV ✓ | binding | **borrower** | Old bank's internet bank or signed application | **no**, only after a bank is chosen. 3 months' notice avoids the fee |
| **Expert valuation (eksperthinnang)** | LTV | all ✓ | **contract** (LHV, Coop, Holm); decision (Cit) | **borrower orders and pays** | Accepted appraiser; PDF / .asice | partly; see the valuation section |
| Land-register extract | ownership, encumbrances | not requested; banks, notaries and appraisers fetch it themselves | – | – | e-kinnistusraamat | registry part number only |
| Floor plans, building design, occupancy permit (kasutusluba) | valuation | via appraiser; SEB ✓ occupancy permit (for purchases); Lum ✓ (construction) | valuation | borrower → appraiser | EHR (building register), own archive | partly; houses need the design + occupancy permit; rented homes need the lease |
| Energy certificate | green pricing, EIS | SEB REFIN26 campaign (A/B classes only) | prelim | borrower / EHR | ehr.ee | **yes** (auto from EHR) |
| Home insurance, bank as beneficiary | collateral | all ✓ (SEB, Holm: rebuild value) | contract / after | borrower | Policy PDF | current policy yes; beneficiary changes later |
| Spouse consent / spouse at notary | Family Law Act | LHV ✓ explicit | notary | borrower + spouse | Notarial deed | **collect marital status + property regime** |
| Co-borrower application + statements | joint income | LHV ✓ (needed even for the first offer), Sw ✓ | prelim | co-borrower | Same as borrower | yes |
| EIS (ex-KredEx) target-group proof | new guarantee | LHV, Coop, Cit, Holm ✓ | prelim → contract | borrower; bank applies | eesti.ee children extract, energy certificate | yes |
| Notarial deeds (mortgage assignment + amendment of secured claims) | security | all ✓ | notary | new bank | Notary (Coop: remote notarisation available) | no |

## Valuation

| Bank | Validity | Accepted firms | Notes |
|---|---|---|---|
| LHV | ≤ 12 months (.asice) | In-house appraiser or recommended firms | €150 found only for *new apartments* (eralaen page). **The "€150 desktop valuation" claim in `03` is unconfirmed** |
| Coop | 6 or 12 months (pages conflict) | 16 listed | Borrower must be the one who orders it |
| Bigbank | 3–6 months (12 by exception) | level 6/7 certified appraiser | – |
| Holm | ≤ 6 months | level 6/7 certified appraiser | – |
| Swedbank | not published | 15 listed | Inspection ≈ 15 min, report in a couple of days |
| SEB | not published | 17 listed | **Rotation rule:** same expert at most twice in a row |
| Citadele | not published | 14 listed | – |
| Luminor | not published | not published | – |

- **On every published list (Sw, SEB, Coop, Cit):** 1Partner, Arco Vara, ERI, Kaanon, Lahe, LVM, Newsec, Ober-Haus, RE Kinnisvara (Harju/Rapla only), Uus Maa.
- **Cost:** a full report is ≈ **€220–460**, depending on region and property type.
- **Reuse:** no bank accepts the old bank's report as such. A recent report from an accepted firm is fine if it is within the validity window.

## Per-bank notes

- **Swedbank:**
  - Apply in the internet bank with purpose "muu" (other) and the amount set to the balance.
  - Requires ≥ €1,000 net income and both applicants' data.
  - Campaign until 31.12.2026 for loans > €100k: Swedbank pays the notary and the contract fee is €0.
  - The contract is signed in the internet bank, then at the notary.
- **SEB:**
  - Fee 0.4% (min €190).
  - REFIN26 (A/B energy classes): SEB pays the notary on loans > €100k.
  - Press estimate: valuation ≈ €250, notary + state fee ≈ €350.
- **LHV:**
  - Process: application → offer within 24 h → borrower checks the old contract's termination terms → contract → notary. LHV pays the notary on refinancing.
  - Statements: 6 months as a sealed .bdoc; links are not accepted.
  - Mortgage is usually 1.3× the loan.
- **Luminor (snippet):**
  - Amount = balance, down payment €0. At least one borrower and one collateral must move.
  - Early-repayment request to the old bank ≥ 10 working days before the notary.
  - Land register updated within a month. 2–4 weeks in total if a valuation exists.
- **Coop:**
  - Statements sealed by the issuing bank. Valuation submitted just before the contract.
  - Coop books the notary; remote notarisation is possible. Any FI-licensed insurer is accepted.
- **Bigbank:** first-rank mortgage only, property ≥ €20k, income ≥ €600 (€950 for a couple). Notary and state fees can be financed. Other documents are "clarified in consultation".
- **Citadele:**
  - 7-step process. Campaign reimburses notary + valuation up to €600.
  - An existing KredEx guarantee is "not an obstacle".
- **Holm:** valuation needed before signing, not at application. First rank only.
- **EIS guarantee:** only one at a time, and it is tied to a specific loan. **Assume it lapses with the old loan, so the new bank applies again** (no official carry-over rule found). Fee is 3% of the guarantee amount (2% for large families).

## Recommended "bank-ready" pack

**Collect up front:**
1. **Identity:** Smart-ID / Mobile-ID login + contact details.
2. **Statements, 6 months, all accounts:** bank-sealed ASiC-E/BDOC preferred, or AIS consent. Plain PDF is the fallback (indicative only). **Refresh before submission.**
3. **Income:**
   - EMTA tax-data certificate (sealed).
   - FIE: last tax return incl. annex E.
   - Dividend / rental / foreign income: tax return + evidence.
4. **Old loan facts:** creditor, **contract number**, balance, margin + Euribor tenor, maturity, fixed-rate period, payment schedule, mortgage amount and rank, early-repayment terms. **Loan contract upload is the main pre-fill source.**
5. **Property:**
   - Registry part number, cadastral number, address, type, energy class (auto from EHR).
   - For houses: design + occupancy permit.
   - If rented out: the lease.
6. **Household:** co-borrower (their own items 1–3), **marital status + property regime**, and whether the spouse will sign.
7. **Liabilities:** other loans, leases, guarantees given, EIS guarantee.
8. **Current home-insurance policy:** insurer, sum insured, expiry.

**Only after a bank is chosen:**
- Binding offer and KYC.
- Valuation (or check the validity of an existing one).
- Early-repayment application to the old bank.
- New contract.
- EIS application.
- Notary (owner + spouse).
- Insurance beneficiary change.
- Payoff and land-register entry (≈ 1 month).

## Uncertainties

- SEB, Swedbank and Luminor publish no full document lists.
- Coop's valuation validity: 6 vs 12 months.
- The LHV €150 desktop valuation is unconfirmed.
- No official EIS carry-over rule.
- Bill 864 SE and AVM-based valuation are not confirmed in force or in use.

## Sources

**LHV**
- Refinancing: https://www.lhv.ee/en/homeloan-refinancing
- Refinancing FAQ: https://www.lhv.ee/en/faq/refinancing-loan
- Home loan FAQ: https://www.lhv.ee/en/faq/home-loan/
- Application form: https://www.lhv.ee/et/kodulaen/taotlus
- Pre-contractual information: https://www.lhv.ee/et/kodulaenu-lepingueelne-teave
- Eralaen (€150 valuation for new apartments): https://www.lhv.ee/et/eralaen

**Swedbank**
- Home loan: https://www.swedbank.ee/private/credit/loans/home?language=EST
- When refinancing pays off: https://blog.swedbank.ee/igapaevased-rahaasjad/eluase/millal-ja-miks-tasub-eluasemelaen-refinantseerida
- Preparing for valuation: https://blog.swedbank.ee/igapaevased-rahaasjad/eluase/millest-soltub-kinnisvara-hind-ning-kuidas-vara-ette-valmistada-vaartuse-hindamiseks

**SEB**
- Home loan: https://www.seb.ee/en/private/loans/home-loan
- Accepted agencies: https://www.seb.ee/en/property-agencies-accepted-seb
- Press release: https://www.seb.ee/foorum/pressiuudised/seb-kodulaenuturul-kaib-aina-tihedam-rebimine-laene-tuuakse-ule-teistest

**Luminor (snippets)**
- Refinancing: https://luminor.ee/kodulaenu-refinantseerimine
- FAQ: https://luminor.ee/kodulaenu-korduma-kippuvad-kusimused
- Interview: https://www.adaur.ee/luminor-viis-koige-sagedasemat-kusimust-kodulaenude-kohta/

**Coop**
- Home loan: https://www.cooppank.ee/en/private/home/home-loan
- FAQ: https://www.cooppank.ee/en/info/faq/home-loan
- Moving a loan to Coop: https://cooppank.ee/ariklient/blogi/mida-peaksin-teadma-kui-soovin-kodulaenu-uude-panka-ule-tuua
- Use of EMTA data: https://www.cooppank.ee/coop-pank-hakkab-kasutama-laenuotsuste-kiirendamiseks-maksuameti-andmeid

**Other banks**
- Bigbank: https://www.bigbank.ee/kodulaen/
- Citadele refinancing: https://www.citadele.ee/et/private/mortgage-refinance/
- Citadele appraisers: https://www.citadele.ee/et/private/mortgage/appraisers/
- Holm: https://www.holmbank.ee/en/private-individual/loans/holm-home-loan

**EIS**
- https://eis.ee/en/services/housing-loan-guarantee/

**Bill 864 SE**
- Riigikogu: https://www.riigikogu.ee/tegevus/eelnoud/eelnou/9035812d-3c51-43e0-b564-d371ce6c7876/
- Ministry of Justice and Digital Affairs: https://www.justdigi.ee/uudised/kodulaenu-teise-panka-viimine-muutub-kiiremaks-ja-odavamaks-ning-loob-voimaluse
- Legal affairs committee: https://www.riigikogu.ee/pressiteated/oiguskomisjon-et-et/oiguskomisjon-toetab-eluasemelaenude-refinantseerimise-lihtsustamist/

**Valuation, AVM and appraisers**
- AVM rule change (fin.ee): https://www.fin.ee/uudised/kodulaenu-tagatise-hindamise-reeglid-muutuvad-paindlikumaks
- Lahe price list: https://www.lahekinnisvara.ee/hindamisakt/

**Notaries' Chamber**
- https://www.notar.ee/et/notarite-koda/uudised/notarite-koda-eluasemelaenu-teise-panka-viimisel-aitab-notar-kaitsta-inimeste

**e-MTA certificates**
- https://www.emta.ee/en/private-client/e-services-tax-literacy/registers-inquiries/applying-certificates

**Family Law Act (PKS)**
- https://www.riigiteataja.ee/akt/13330603
