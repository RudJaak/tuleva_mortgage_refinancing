# Research: Estonian eID login in a local hackathon setup

_Researched 2026-10-10. The SK DEMO endpoints were probed live on this date. Items marked [unverified] could not be confirmed._

## Short answers

| Question | Answer |
|---|---|
| **Real phone + real Smart-ID account against production?** | **No.** It needs a paid SK relying-party (RP) contract (from €60/month minimum, €0.109 per transaction, price list valid from 1 June 2026) or someone else's RP credentials. There is no free or self-serve route. |
| **Real phone against SK DEMO?** | **Yes, free.** Install the separate **Smart-ID demo app** and register with your real ID card or Mobile-ID. You get a DEMO *Qualified* account under your own isikukood. Tuleva's QR flow then works from `http://localhost`. |
| **No phone at all?** | **Yes.** The DEMO test accounts auto-complete. Verified live: Smart-ID notification flow with `PNOEE-40504040001` → `COMPLETE/OK`; Mobile-ID `+37268000769 / 60001017869` → `COMPLETE/OK`. |
| **Real Mobile-ID SIM against DEMO?** | **No longer possible.** `demo.sk.ee/MIDCertsReg/` returns 404 and is not documented any more. |
| **Real ID card on localhost?** | **Yes**, via Web eID. It needs an https origin (or the Web eID extension v2.5's `ALLOW_HTTP_LOCALHOST` dev option), a card reader, and AIA OCSP, which needs no contract. |

## Smart-ID with your own phone against DEMO

Official page: https://sk-eid.github.io/smart-id-documentation/demo.html

1. **Install the demo app.** It is a separate app from the production Smart-ID app.
   - Android: https://play.google.com/apps/testing/com.stagnationlab.sk
   - iOS: TestFlight, https://testflight.apple.com/join/DoWddZjR
2. **Register with ID card or Mobile-ID.** This gives a **Qualified** account with your real isikukood.
   - Registering via bank link only gives a *Basic* account, and the DEMO relying party can't use Basic accounts.
   - Manual registration with an Estonian code isn't possible: "Other country" gives a random code.
3. **Set PIN1 and PIN2, then check the account** at https://sid.demo.sk.ee/portal/login.
4. **Fix the certificate check.** DEMO certificates aren't automatically known to the DEMO AIA OCSP, and Tuleva's `SmartIdCertificateRevocationCheck` treats UNKNOWN as revoked. Either:
   - download your demo certificate from the portal and upload it as GOOD at https://demo.sk.ee/upload_cert/, or
   - turn the revocation check off in our fork.

## Smart-ID RP API v3 flows on localhost

| Flow | Callback needed? | Works with an `http://localhost` frontend? |
|---|---|---|
| **Device-link, cross-device QR** | No. The phone only talks to SK | **Yes** |
| Device-link, same-device (Web2App) | Yes, and it must be **https**. The phone opens it, so the laptop's localhost is useless | Only with https reachable from the phone (mkcert + LAN IP, or a tunnel) |
| Notification by personal code | No | Yes, but SK discourages it (see below) |

- **`initialCallbackUrl` must match `^https://…`.** Tuleva always sets a callback on the device-link session (one session serves both QR and Web2App). So set `smartid.callback-url` to an https string such as `https://localhost:5173/login/smart-id/callback`. It is never called in the QR flow.
- **The QR code must be refreshed every second.** `authCode` is an HMAC-SHA256 over the session secret, computed in the backend.
  - Tuleva's backend: `GET /v1/smart-id/login/qr-code` (`SmartIdDeviceLinks.qrCodeLink`).
  - Tuleva's frontend: `useSmartIdQrCodeLink.ts` polls every 1,000 ms.
  - DEMO needs `scheme-name: smart-id-demo`.
- **Notification login is discouraged.** It still exists in v3, but SK says to use it only for devices that have already completed a device-link flow. Tuleva follows this: push login only for a remembered browser, and there is no personal-code entry. **To use the auto-completing DEMO accounts we need a small dev-only endpoint** that calls `createNotificationAuthentication().withSemanticsIdentifier(...)`.
- **DEMO accounts in the device-link flow** (`PNOEE-40404040009-MOCK-Q`) don't complete on their own. The backend must `POST https://sid.demo.sk.ee/mock/device-link` with `{documentNumber, deviceLink}`.

## Test identities (no phone)

- **Smart-ID notification** (auto-complete):
  - `40504040001` (DEM2-Q)
  - `50001029996`
  - `39901012239` (MOCK-Q)
  - `61101012257`
  - Separate codes produce the USER_REFUSED, WRONG_VC and TIMEOUT outcomes.
- **Smart-ID device-link via the mock service:** `40404040009`, `61101019999`.
- **Mobile-ID** (auto-complete): `+37268000769 / 60001017869`, `+37269930366 / 51307149560`. Base URL `https://tsp.demo.sk.ee/mid-api`.
- Sources:
  - https://sk-eid.github.io/smart-id-documentation/test_accounts.html
  - https://github.com/SK-EID/MID/wiki/Test-number-for-automated-testing-in-DEMO

## TLS / pinning

- SK's pinning page lists certificates for `sid.demo.sk.ee` and `rp-api.smart-id.com` that **expire 2026-10-11**.
- The live servers already serve new certificates (to March 2027, DigiCert Global G2 TLS RSA SHA256 2020 CA1).
- **Tuleva's `test_keys/truststore.jks` contains that intermediate** and validates sid.demo, tsp.demo, rp-api and mid.sk.ee. Reuse it as is.

## ID card via Web eID

- **`authtoken-validation` requires an https origin.** Options:
  - (1) Serve Vite over https (mkcert or basic-ssl) with `web-eid.origin=https://localhost:5173`.
  - (2) Use Web eID extension **v2.5.0**'s DevTools option `ALLOW_HTTP_LOCALHOST`, which rewrites the origin to https before signing [whether the store builds are on v2.5 is unverified].
- **Install:** the Web eID native app and extension (https://web-eid.eu), plus a card reader.
- **OCSP:** the documented path for production cards needs no configuration, but AIA has no SLA. Cards issued from 17.11.2025 use `ocsp.eidpki.ee`; older cards use `aia.sk.ee/esteid2018`. Tuleva's truststore already has the ESTEID2018 and ESTEID2025 certificates.

## Aggregators (no own SK contract)

**Every sandbox runs on SK DEMO.** None gives a same-day, free, real-phone production login.

| Option | Sandbox | Production |
|---|---|---|
| **SK Authentigate** (SK's own OIDC broker) | Free demo plan via portal.skidsolutions.eu | Starter plan €29/month, contract |
| **eID Easy** | Self-serve at test.eideasy.com; real identities don't work there | Usage-based; via sales [unverified] |
| **Signicat** (incl. Dokobit) | Free sign-up; SK demo users | Needs an onboarding manager for Smart-ID |
| **Idura** (ex-Criipto) | – | Estonian Smart-ID no longer listed |

## Options for the demo, ranked

| # | Option | Effort | Audience sees | Risks |
|---|---|---|---|---|
| A | **DEMO test accounts** (Smart-ID notification via a dev endpoint, Mobile-ID as is) | 1–2 h | Verification code, then logged in as a test person | Needs internet; DEMO has no SLA; not a real identity |
| B | **Your real phone against DEMO** (demo app + QR) | +30–60 min on top of A | A live QR on localhost, you scan it, the app shows DEMO, you enter PIN1, you're logged in **as yourself** | TestFlight or Play beta access, registration hiccups, OCSP UNKNOWN |
| C | **Real ID card via Web eID** | 1–2 h | PIN1 dialog, logged in as yourself with your production identity | Reader and driver, extension version, AIA availability |
| E | **Tuleva's production RP credentials** | ~30 min technically | Real Smart-ID app, but the screen says **"Tuleva"** | Billed to Tuleva; real personal data under Tuleva's SK contract and GDPR role; secrets on a laptop; SK terms for third-party use unverified. **Needs written OK from Tuleva, ideally SK too** |
| D | Aggregator | Sandbox in hours | Same as A | Adds nothing over A; production takes days or weeks |

## Sources
- **Smart-ID documentation**
  - Demo app and environment: https://sk-eid.github.io/smart-id-documentation/demo.html
  - Environments: https://sk-eid.github.io/smart-id-documentation/environments.html
  - FAQ: https://sk-eid.github.io/smart-id-documentation/faq.html
  - Device-link flows: https://sk-eid.github.io/smart-id-documentation/rp-api/device_link_flows.html
  - Mock service: https://sk-eid.github.io/smart-id-documentation/rp-api/mock_service.html
  - Endpoint overview: https://sk-eid.github.io/smart-id-documentation/rp-api/overview_of_api_endpoints.html
  - HTTPS pinning: https://sk-eid.github.io/smart-id-documentation/https_pinning.html
- **SK pricing:** https://www.skidsolutions.eu/?p=1624 ; ordering: https://www.smart-id.com/e-service-providers/
- **Mobile-ID:** https://github.com/SK-EID/MID/wiki
- **Web eID:**
  - Overview: https://web-eid.eu
  - Browser extension: https://github.com/web-eid/web-eid-webextension
  - ID-card OCSP endpoints: https://www.id.ee/en/article/thales-id-card/
- **Tuleva code** (local copy, read-only, in the scratchpad):
  - `onboarding-service/src/main/java/ee/tuleva/onboarding/auth/smartid/`: `SmartIdLoginStarter`, `SmartIdDeviceLinks`, `SmartIdLoginController`
  - `onboarding-service/src/main/java/ee/tuleva/onboarding/auth/webeid/WebEidConfiguration.java`
  - `onboarding-client/src/components/login/smartId/useSmartIdQrCodeLink.ts`
