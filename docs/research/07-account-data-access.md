# Research: getting account-statement data from Estonian banks

_Researched 2026-10-10. Items marked [unverified] come from secondary sources or could not be confirmed._

Banks want **6 months of statements for all accounts**. Our MVP and universal fallback is a **PDF upload parsed by an LLM**. This note looks at lower-friction and higher-trust options.

## TL;DR

- **Enable Banking** (a Finnish AISP) is the best AIS route for the hackathon and production. Its live ASPSP list covers all nine target banks in Estonia:

  | Bank | Status |
  |---|---|
  | Swedbank | live |
  | SEB | live |
  | LHV | beta |
  | Luminor | beta |
  | Coop | live |
  | Bigbank | live |
  | Citadele | beta |
  | Revolut | live |
  | Wise | live |

  - Consent can last up to 180 days.
  - The sandbox is free and self-serve.
  - **"Restricted production" lets team members link their own real accounts for free**, so we can do a real Smart-ID demo.
  - Enable Banking acts as the regulated entity. Tuleva would need a contract and KYB, **not its own licence**.
- **Six months of history is the catch.**
  - PSD2 rules only waive SCA for the last **90 days**. Older history needs a second SCA, and many banks only release it for a short window after authorisation:
    - Luminor: 5 minutes.
    - Enable Banking FAQ: about 1 hour.
  - **Design rule:** fetch the full 6 months immediately in the callback, and warn the user that their bank may ask them to confirm twice.
- **Signed statements exist.** Swedbank and Coop internet banks export **ASiC-E** containers sealed with the bank's digital stamp. We can verify these with **digidoc4j**, which onboarding-service already uses. This is the best authenticity option short of AIS.
- **Dead or closed options:**
  - GoCardless Bank Account Data (ex-Nordigen): no new sign-ups since July 2025.
  - Kevin: bankrupt, licence revoked September 2024.
  - Creditinfo Eesti's AIS: stopped May 2025.
- **No Baltic bank has an MCP server.** The "European bank with MCP" is **Danske Bank**, piloting for corporate customers only. Community MCP servers on top of aggregators exist, e.g. `github.com/elcukro/bank-mcp` (supports Enable Banking). They are irrelevant to our flow.

## Recommended ladder (best first)

| # | Channel | Coverage | Friction | Authenticity | Hackathon |
|---|---|---|---|---|---|
| 1 | **AIS via Enable Banking** | All 9 EE banks | Low: redirect + Smart-ID, with a 2nd SCA for history older than 90 days | Highest (bank-sourced) | ✅ sandbox plus own-account restricted mode |
| 2 | **ASiC-E sealed statement upload** | Swedbank, Coop (others unknown) | Medium | High: cryptographically verifiable (digidoc4j) | ✅ |
| 3 | **camt.053 XML upload** (ISO 20022, Pangaliit guide) | LHV (camt.053.001.02), SEB ("ISO väljavõte"), Swedbank (retail unclear), Luminor (business) | Medium | Parse ≈ 99%+, deterministic. Not authenticated | ✅ |
| 4 | **CSV upload** | All | Medium | ≈ 97–99% with a mapper per bank; schemas drift (LHV changed its CSV on 25.02.2026). Not authenticated | ✅ |
| 5 | **PDF upload + LLM** (MVP / fallback) | All | Medium | ≈ 90–98% field accuracy, forgeable. **Always reconcile opening balance + transactions = closing balance** | ✅ (MVP) |
| 6 | Screenshot + LLM | All | High | ≈ 80–95%, incomplete | ✅ (last resort) |

**For income specifically,** the EMTA declared-income certificate ("tõend deklareeritud tulu kohta", from e-MTA) is an official cross-check. Banks already pull EMTA wage data under a 2019 EMTA–Pangaliit arrangement.

**Proposal for the build:**
- Build the upload pipeline once. It should detect the format (ASiC-E → verify the seal and unwrap the inner file; XML → camt parser; CSV → per-bank mapper; PDF/image → LLM).
- Reduce every result to the **same derived summary**: monthly net income, recurring loan and leasing payments, and arrears or overdraft signals.
- Add Enable Banking AIS as a **parallel "connect your bank" button** if time allows.

## Aggregator comparison

| Provider | Estonian coverage | Access | Licence model | Notes |
|---|---|---|---|---|
| **Enable Banking** | All 9 (+ N26, bunq, Paysera). Not Inbank or Holm | Free sandbox; self-serve; restricted production for own accounts | Enable Banking is the regulated entity; contract + KYB | Volume pricing per account per month with a minimum (quote via info@enablebanking.com). "Most banks provide ≥ 1 year history" |
| Salt Edge | Not verified | Partners start in sandbox until approved | Partner Program + "Partner Consent" | Sales-led |
| Tink (Visa) | "Live in the Baltics for current customers"; new onboarding "very soon" | Sandbox | Ask sales | Has Income Check; EE availability unknown |
| Yapily | SEB Baltics (EE sandbox) | Sandbox | Yapily Connect UAB (licence-as-a-service, Bank of Lithuania) | Material is UK-centric |
| **Accountscoring** (OÜ Krediidiregister) | EE AISP licensed by FI since 2018; EE/LV/LT | Sales | It is the AISP | **Local, lender-focused**: categorisation and scoring. A good production alternative |
| Montonio | Holds an AIS + PIS licence (Bank of Lithuania) | No public AIS product | – | **Tuleva already uses Montonio.** Worth asking whether they'd expose AIS |
| Kreditz (SE) | Baltics not found | Sales | AISP | Income-verification focus |
| GoCardless BAD | – | **Closed to new sign-ups** | – | – |
| Kevin | – | **Dead** | – | – |
| TrueLayer, Aiia | No confirmed Baltic coverage | – | – | Skip |

**Banks' own PSD2 portals.** Production use needs an FI licence plus eIDAS QWAC/QSEAL certificates, so going direct is not an option for Tuleva.
- **Swedbank:** has "Premium APIs" (no licence, but a business agreement is needed).
- **LHV:** the sandbox at `api.sandbox.lhv.eu` is open to everyone. Long-term consent covers 90 days of history; short-term consent (30 minutes) covers older history.

**Redirect UX.** Every Estonian bank redirects the user and confirms with Smart-ID or Mobile-ID; most also allow ID-card, the bank app or a PIN calculator. Coop has no decoupled flow. Several banks require the `psu-ip-address` and `psu-user-agent` headers.

## Internet-bank exports by bank

| Bank | Retail export formats | Signed? |
|---|---|---|
| Swedbank | XLS, CSV, PDF, **ASiC-E**; ISO XML option [unverified for private accounts] | **Yes** (digital stamp, "digitempel") |
| LHV | PDF, CSV (new 16-column format from 25.02.2026), XML camt.053.001.02 | Not documented |
| SEB | "ISO väljavõte" (kvv.xml); documented mostly for business | Not found |
| Coop | PDF, **ASiC-E**, CSV, XML, TH6 (business internet bank) | **Yes** |
| Luminor | Retail formats not found; business: PDF/CSV/XML, camt.053, MT940 | Partially |
| Citadele | CSV, PDF, XML (camt.052) in LV; EE presumably similar [unverified] | Unknown |
| Revolut, Wise | PDF, CSV | No |

## Other channels

- **RIA consent service:** covers **state registry data only** (over X-Road). Not bank data. It could deliver EMTA income data, but needs X-Road membership and an agreement with RIA. Not feasible for the hackathon.
- **FiDA:** not adopted; stalled since June 2025. Irrelevant before about 2028.
- **PSD3/PSR:** provisional deal reached 27.11.2025. Expected to apply about 2027–28.
- **Screen-scraping:** not allowed. **Never collect bank credentials.**

## Legal and GDPR

- **Options:**
  - (1) A licensed AISP delivers the data to Tuleva. This is the production route. Confirm with the provider, and ideally with FI, that the model covers an unlicensed recipient in Estonia.
  - (2) The user downloads and uploads the statement themselves. No PSD2 issue, only GDPR.
  - (3) Tuleva registers as an AISP itself. That takes months.
- **Legal basis:** under GDPR it is Art 6(1)(b), steps before a contract. PSD2 "explicit consent" is a contractual requirement, not the GDPR basis (EDPB 06/2020).
- **Counterparties ("silent parties")** in the transactions: legitimate interest only, minimised, no reuse.
- **Special-category data:** transactions can reveal union membership, health or religion. **Store only derived aggregates; discard raw transactions after extraction.**
- A DPIA is likely needed in production (large-scale financial profiling).

## Enable Banking sandbox: integration steps (Spring Boot)

1. **Create the app.** Sign in at enablebanking.com, go to Control Panel → API applications, and add an app with environment **Sandbox** and redirect `http://localhost:8080/callback`. Download `<app-id>.pem`.
2. **Authenticate each request with a JWT.**
   - Algorithm RS256. Header: `{typ:"JWT", alg:"RS256", kid:<app-id>}`.
   - Claims: `{iss:"enablebanking.com", aud:"api.enablebanking.com", iat, exp:iat+3600}`, signed with the PEM (PKCS8; nimbus-jose-jwt or jjwt).
   - Send it as a Bearer token to `https://api.enablebanking.com`.
3. **List the banks:** `GET /aspsps?country=EE` returns each bank, its maximum consent validity and the required PSU headers.
4. **Start authorisation:** `POST /auth` with `{access:{valid_until}, aspsp:{name, country:"EE"}, state, redirect_url, psu_type:"personal"}`. Redirect the user to the returned `url`.
5. **Handle the callback:** it returns `?code&state`. Call `POST /sessions {code}`, which returns a `session_id` and the accounts (`uid`). Persist anything that is returned only once.
6. **Fetch the data immediately:**
   - Call `GET /accounts/{uid}/balances` and `GET /accounts/{uid}/transactions`, paging back 6 months.
   - The parameter names (`date_from`, `continuation_key`) are [unverified]; check the API reference.
   - Forward the PSU headers.
7. **Sandbox test credentials** are shown on each bank's sandbox page (e.g. `customera` / `12345678` in the quick-start) [unverified per bank].
8. **Real-data demo:** register a second app with environment Production, choose "Activate by linking accounts", and link team members' own accounts. This is for evaluation and non-commercial use only.

## Sources

- **Enable Banking**
  - Estonia market notes: https://enablebanking.com/docs/markets/ee/
  - Quick start: https://enablebanking.com/docs/api/quick-start
  - Restricted production / linked accounts: https://enablebanking.com/docs/api/linked-accounts/
  - FAQ: https://enablebanking.com/docs/faq/
  - Live ASPSP list (JSON): https://enablebanking.com/api/aspsps
  - API reference: https://enablebanking.com/docs/api/latest/
- **Other aggregators**
  - GoCardless closure: https://actualbudget.org/docs/advanced/bank-sync/gocardless/
  - Salt Edge Partner Program: https://www.saltedge.com/partner_program
  - Tink FAQ: https://tink.com/faq/
  - Yapily Connect: https://docs.yapily.com/tools-and-services/yapily-connect/overview
  - Montonio licence (Bank of Lithuania): https://www.lb.lt/en/licences-1/view_license?id=1983
  - Kevin licence revoked: https://www.lb.lt/en/news/payment-institution-licence-of-kevin-eu-uab-revoked
  - Creditinfo AIS withdrawal: https://fi.ee/en/news/finantsinspektsioon-has-withdrawn-operating-licence-payment-institution-creditinfo-eesti
  - Accountscoring: https://accountscoring.com/about?locale=en
- **Banks**
  - Swedbank open banking: https://www.swedbank.com/openbanking/faq.html
  - Swedbank statement exports: https://www.swedbank.ee/private/d2d/accounts/info?language=EST
  - LHV open banking: https://partners.lhv.ee/en/open-banking/
  - LHV export formats: https://www.lhv.ee/et/internetipanga-vormingud
  - Coop open banking: https://www.cooppank.ee/en/open-banking-api-interface-coop-pank-open-service-providers
  - Coop service channels: https://www.cooppank.ee/en/business/daily-banking/service-channels
  - Export guides (third-party): https://tttarkvara.ee/andmevahetus-pankade-internetikeskkondadega/ , https://www.smartaccounts.eu/et/2021/01/pangamaksete-import/
- **MCP**
  - Danske Bank MCP pilot: https://thepaypers.com/fintech/news/danske-bank-pilots-mcp-server-for-corporate-ai-banking-access
  - Agentic banking directory: https://www.openbankingtracker.com/agentic-banking-and-mcp
- **Estonian state services**
  - ASiC-E verification: https://www.id.ee/?id=37273
  - EMTA data for bank lending decisions: https://emta.ee/en/news/banks-lending-decisions-will-be-based-tcb-data-applicants-real-income
- **EU regulation**
  - FiDA: https://data.consilium.europa.eu/doc/document/WK-5041-2026-INIT/en/pdf
  - PSD3/PSR deal: https://www.taylorwessing.com/en/insights-and-events/insights/2025/11/eu-lawmakers-strike-a-deal-on-payments-reforms
  - EBA RTS 180-day amendment: https://www.eba.europa.eu/eba-publishes-final-report-amendment-its-technical-standards-exemption-strong-customer
