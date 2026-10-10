# 11 · Credit report on the applicant via API

**Question:** can the flow request an Estonian credit report on the borrower through an API as its first step, before asking for any documents?

**Short answer:** technically yes, but not for us, and it isn't worth doing first.

- **Technically:** Creditinfo Eesti queries its payment-default register by personal code, in real time and for about €1, over SOAP or REST.
- **Legally:** the query needs a contract with Creditinfo and a legal basis for each query. A licensed credit intermediary or a bank has that basis (KAVS § 50(1), VÕS § 403⁴(3)). Tuleva as a non-licensed operator would have only a weak legitimate interest, or the person's consent.
- **Value:** a report adds only **negative** data: payment defaults, tax debts and court notices. It shows no active loans. Positive data (the KJS register) is not open to anyone until 1 March 2029, and never to intermediaries. Our 6-month statements already show other loans, late fees and debt collection (D26).
- **For the demo:** put the check behind a `CreditCheckProvider` interface with a stub, as an optional pre-screen after login with explicit consent.
- **For a real launch:** have the licensed partner run the query, or let the borrower share their own report from Minu Creditinfo.

_Researched 2026-10-10 from Riigi Teataja (current consolidated texts), AKI, and Creditinfo's own documents. Creditinfo's Minu Creditinfo page was read from its public web bundle. **This is research, not legal advice.**_

## 1. Who holds credit data on private persons in Estonia

| Provider | Data on a private person | Machine access | Who may query | Price (public) |
|---|---|---|---|---|
| **Creditinfo Eesti AS** (maksehäireregister, founded 2001 by the Estonian banks) | Payment defaults (≥ €30, > 45 days overdue) with history, creditor's sector, start and end dates and amounts. Credit score: probability of a default in the next 12 months. Tax debts from EMTA (live), official notices (bankruptcy, enforcement auctions), business links and bans, number of properties, population-register basics | **Data Market** (REST) and **KiReport** (SOAP) web services, and the e-Krediidiinfo portal. Technical wiki at `services.krediidiinfo.ee` | Contract customers only. The contract (§ 2) says: "Füüsiliste isikute andmete pärimine on lubatud üksnes õigustatud huvi olemasolul" (querying private persons is allowed only with a legitimate interest) | e-Krediidiinfo list price, ex VAT: payment-default report €1.20, credit report €6.50, fact report €4.10. API price by contract [not public] |
| **Krediidiregister OÜ** (taust.ee; ownership linked to the Julianus Inkasso group) | Its own payment-default register (it claims to be "Estonia's largest"), private-person credit score and "Krediidiskoor+". It also holds an account-information (AIS) licence (Accountscoring) | Web portal plus "integrations" (API exists, docs not public) | Banks, lenders and companies. The price page doesn't state a legal-basis rule [unverified] | taust.ee: €5 single query; Premium €49/month for 25 queries; Pro on request |
| **Dun & Bradstreet Estonia AS** | Company data. **No evidence of a private-person default product** | Company APIs | n/a | n/a |
| **Inforegister** (Register OÜ) | Company data; it masks personal codes. No private-person default lookup found | n/a | n/a | n/a |
| **KJS positive credit register** (Krediiditeabe jagamise seadus, in force 30.06.2026) | Every consumer credit contract: balance, next instalment, delays over 14 days, guarantors (§ 7) | Register operator to be appointed by the state | **Only credit institutions and licensed creditors**, plus courts and authorities (§ 10(1)). **Credit intermediaries are not on the list.** Access from **1 March 2029** (§ 58(3)) | Regulated fee schedule (§ 13) |

Notes:
- **Two registers mean neither is complete.** Creditors choose which register they report a default to.
- **Retention differs by source:**
  - IKS § 10(2)(5) forbids sharing a default more than 5 years after the breach ended.
  - Creditinfo's privacy notice (§ 8.8) says the same.
  - Its 2024 leaflet says unpaid private defaults are "generally published for 15 years". Unpaid defaults have not "ended", so both can hold.
- **Our query becomes part of the person's data.** Creditinfo's score uses "päringud Andmesubjekti kohta" (enquiries about the person) as an input (privacy notice § 9.7.10). Every query is shown to the person in Minu Creditinfo with the requester's name and the legal basis.
  - There is no documented "soft search" that leaves no trace. [unverified whether one enquiry moves the score]

## 2. Legal basis to query

- **IKS § 10(1)** (verified in the current text):
  - Default data may be passed to a third party only "andmesubjekti krediidivõimelisuse hindamise eesmärgil või muul samasugusel eesmärgil" (to assess the person's creditworthiness, or for a similar purpose).
  - The data must have been checked, and the transfer must be logged.
  - Sharing is barred when the breach is under 30 days old or more than 5 years past its end, or when it would excessively harm the person (§ 10(2)).
- **Creditinfo's own basis** is GDPR Art 6(1)(f), the *recipient's* legitimate interest. It shares data only with recipients who have a "põhjendatud vajadus ja vastav õiguslik alus" (a justified need and a matching legal basis; privacy notice § 8.3).
- **AKI** (FAQ "Maksehäired"):
  - "Maksehäireregistrist ei saa igaüks küsida andmeid teiste inimeste võlgnevuste kohta" (not everyone may ask the register about other people's debts).
  - A credit institution has a legitimate interest before granting a loan.
  - Consent also works if it is "vabatahtlik, selgesti mõistetav ning ühemõtteline" (voluntary, clearly understood and unambiguous).
  - The person may object at any time, and the register must then redo the balancing test.
  - AKI's legitimate-interest guide (2020, updated 2022) requires a documented three-step balancing test.
- **Creditinfo's description of valid bases**, as shown to the person in Minu Creditinfo: the person's consent, a legal obligation, entering into or performing a contract with the person, or the requester's legitimate interest.
- **For a licensed mortgage credit intermediary the basis is explicit:**
  - KAVS § 50(1): the creditor *or intermediary* obtains creditworthiness information "tarbijalt, asjakohastest siseallikatest ja andmekogudest" (from the consumer, internal sources and databases).
  - § 47(1) puts the creditworthiness duties on intermediaries too.
  - § 49(5) lets the bank rely on the intermediary's assessment.
  - § 50(2) requires the intermediary to pass everything to the bank.
- **For Tuleva without a licence:**
  - There is no statutory duty, so the basis would be Art 6(1)(a) consent or 6(1)(b) steps at the person's request.
  - Creditinfo decides whether to sign a contract with a non-lender for this purpose. [unverified; ask Creditinfo]
  - Doing creditworthiness work at all is the activity that makes us an intermediary (05-legal-regulatory, § 1). A credit check strengthens that classification; it doesn't sidestep it.
- **Art 22 GDPR:**
  - If a found default automatically blocks someone from joining the pool, that is an automated decision with a significant effect. It needs a human review route, or the gate should stay informational.
  - VÕS § 403⁴(10) shows the same pattern for lenders: tell the person, free of charge, which database caused a refusal.

## 3. The person's own report

- **Minu Creditinfo** (minucreditinfo.ee, login through Dokobit with Smart-ID, Mobile-ID or ID card):
  - Any person can see **their own payment defaults free of charge**.
  - It also lists **who queried them in the last two years**, both legal and private persons, with the stated legal basis.
  - It offers paid monitoring ("Minu seire") and dispute filing.
- **"Minu maksehäire raporti jagamine"** (share my payment-default report) is a paid product.
  - The person enters a recipient's e-mail, and Creditinfo sends the report to that recipient.
  - The shared report is in Estonian only.
  - The price is not visible without logging in. [unverified]
  - **This is the cleanest consent-based route:** the data comes straight from the register, it is initiated by the person, and the recipient needs no contract.
- **A consent-request flow exists for private buyers.** A person can buy a report on another private person only after that person approves the request in "Minu nõusolekud".
  - We found **no public API** that lets a *company* receive a report on this consent basis. [ask Creditinfo whether Data Market supports it]
- **Uploading a screenshot or PDF of one's own Minu Creditinfo page:**
  - It is possible, and the borrower provides it voluntarily.
  - It is **not verifiable**: no seal, and easily edited. That conflicts with the "verifiable data" rule (STS Art 20(10)).
  - A "no defaults" result is the only interesting outcome, and the hardest to prove from a screenshot.
- **GDPR Art 15 access request to Creditinfo:** free, but answered within a month. Too slow for the funnel.

## 4. Data protection constraints

- **Purpose limitation.** Use default data only to decide whether a borrower can be included in the pool and to set their indicative risk class. Never use it for marketing, and never re-query "to keep data fresh" without a new trigger.
- **Minimisation.** Store a derived flag, for example `activeDefaults`, `defaultsLast5y` and `checkedAt`, not the full report.
  - **The ESMA template has no field for bureau data.** The relevant pool fields are about arrears on the loan itself.
  - **The pseudonymised tender pool carries only the risk class**, never the default details.
- **Transparency** (GDPR Art 13–14, AKI letter of 29.06.2026 on default notices). Before the query, tell the person:
  - which register is queried and why;
  - that the query will appear in their Minu Creditinfo log;
  - what happens if a default is found.
- **Timing.** A query on the first screen, before the person has shown any real intent, is the hardest to justify under a balancing test. Placing it after the person has entered their loan, or right before signing, makes the purpose concrete.
- **Retention.** Keep the result no longer than the pool round. If a licensed partner runs the check, KAVS § 47(5) requires it to keep the credit file for 3 years after the relationship ends.

## 5. Is it worth being the first step?

**For a first-step check:**
- It is cheap (≈ €1), instant and needs only the personal code we already have from login.
- It rejects early the people the banks will reject anyway, which saves them uploading documents.

**Against:**
1. **Legal basis.** We have none until a licensed partner exists.
2. **Friction and trust.** A credit check before any value is shown deters people. The query also leaves a visible trace in their Creditinfo log.
3. **Little added value:**
   - Mortgage holders with active defaults are a small minority. [modelled; no Estonian figure found]
   - The statements already reveal debt collection, enforcement and late fees (D26).
   - The register shows no active debts, which are what matter for DSTI.
4. **The banks will run their own check at the offer stage anyway.** Our pre-screen can't replace theirs.

## Recommendation

**Hackathon demo:**
- Don't make it the first step, and don't call a real bureau.
- Add a `CreditCheckProvider` interface with a `StubCreditCheckProvider`:
  - It returns a synthetic result: `NO_DEFAULTS`, or `ACTIVE_DEFAULT` for a reserved demo personal code.
  - Show it as an optional pre-screen after login, behind an explicit consent checkbox that names the register and the purpose.
  - Show the result as a badge that feeds the indicative risk class. It is informational, never an automatic block.
- Label it "näidis" (sample) in the UI.
- This would be a scope change, so record it as a new decision in PLAN.md first.

**Real launch:**
1. **Route A, preferred: the licensed partner queries Creditinfo.**
   - The licensed intermediary or bank runs the query through Data Market (REST) or KiReport (SOAP), under KAVS § 50(1) and its legitimate interest.
   - The query happens at the point the person commits, not on landing.
   - Only the derived flag is stored.
   - Optionally query Krediidiregister as well, for coverage.
2. **Route B, without a licence: the person shares their own report.**
   - The person uses Minu Creditinfo's "share my report" to send it to our e-mail address.
   - This is consent-based, comes directly from the source, and needs no bureau contract.
   - Ask Creditinfo for the price, and whether a company-side consent API exists.
3. **KJS positive data comes only via the banks from 1 March 2029.** Design the pool so the chosen bank does that check, not us.

## Confidence

| Finding | Confidence | Basis |
|---|---|---|
| Statutory texts (IKS § 10; KJS §§ 3, 5, 7, 10, 58; KAVS §§ 47–50; VÕS § 403⁴) | High | Verified in the current Riigi Teataja texts |
| Creditinfo's data fields, legal bases and legitimate-interest clause | High | Creditinfo's own documents: privacy notice of 02.03.2026, service contract, product sheet |
| Creditinfo's prices | Medium | The public e-Krediidiinfo price list is portal pricing; API pricing is not public |
| Minu Creditinfo's features (free self-view, query log, paid report sharing, consent flow) | Medium | Read from the site's UI strings, not from a logged-in session. Share price unknown |
| Krediidiregister and taust.ee | Low–medium | Marketing pages and the business register only. API docs and legal-basis rules not seen |
| D&B and Inforegister having no private-person default product | Medium | Absence of evidence only |
| Whether Creditinfo would contract with a non-lender for this purpose, and whether a company-side consent API exists | Unknown | Needs a call to Creditinfo (info@creditinfo.ee, +372 665 9600) |

## Sources

- **Laws (Riigi Teataja):**
  - IKS: https://www.riigiteataja.ee/akt/130062026052
  - KJS: https://www.riigiteataja.ee/akt/120062026004
  - KAVS: https://www.riigiteataja.ee/akt/120062026024
  - VÕS: https://www.riigiteataja.ee/akt/120062026018
- **AKI (Andmekaitse Inspektsioon):**
  - FAQ "Maksehäired": https://www.aki.ee/isikuandmed/kkk/maksehaired
  - Letter on default notices, 29.06.2026: https://www.aki.ee/sites/default/files/documents/2026-06/Vastus%20selgitustaotlusele_makseh%C3%A4ired_l%C3%A4bipaistvus_avaldamisest%20teavitamine.pdf
  - Legitimate-interest guide: https://www.aki.ee/sites/default/files/dokumendid/oigustatud_huvi_juhend_aki_26.05.2020.pdf
  - Precept against Krediidiregister, 2022: https://www.aki.ee/sites/default/files/ettekirjutus-hoiatus_isikuandmete_kaitse_asjas_19.01.2022_nr_2.1.-6_22_1_krediidiregister_ou.pdf
- **Creditinfo Eesti:**
  - Privacy principles, 02.03.2026: https://web.creditinfo.ee/isikuandmete_tootlemise_pohimotted_020326.pdf
  - Service framework contract: https://web.creditinfo.ee/raamleping_ci.pdf
  - Private-person data sheet: https://web.creditinfo.ee/eraisikuandmed_ci.pdf
  - Register leaflet, 2024: https://creditinfo.ee/wp-content/uploads/sites/58/2024/02/maksehaireregister_est.pdf
  - Data-sharing APIs: https://creditinfo.ee/en/avoid-debts/data-sharing-solutions/
  - Technical wiki: http://services.krediidiinfo.ee/wiki/index.php/Main_page_en
  - Price list: https://www.e-krediidiinfo.ee/hinnakiri
  - Minu Creditinfo: https://www.minucreditinfo.ee
- **Krediidiregister OÜ:**
  - Site: https://krediidiregister.ee
  - taust.ee prices: https://www.taust.ee/hinnad
  - Business-register profile: https://www.inforegister.ee/12400621-KREDIIDIREGISTER-OU
- **Related notes:**
  - `05-legal-regulatory.md` (intermediation, KJS)
  - `07-account-data-access.md` (Creditinfo's AIS stopped in May 2025)
