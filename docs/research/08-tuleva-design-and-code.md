# Research: Tuleva design language, calculators, code reuse, auth and signing

_Researched 2026-10-10 against HEAD of the TulevaEE repos. All three repos (`onboarding-service`, `onboarding-client`, `wordpress-theme`) deploy from the **`master`** branch. In raw GitHub URLs, use `/master/`._

## Licences and permission

**Update 2026-10-10:** Tuleva has **authorised the team to use all of its resources**: code, design, website theme, calculators and logo. We are part of the team for the hackathon. This overrides the licence-only analysis below.

| Repo | Licence | Reuse |
|---|---|---|
| `onboarding-service` | MIT © 2017 Tuleva | ✅ Copy, keeping the MIT notice |
| `onboarding-client` | MIT © 2017 Tuleva | ✅ Copy, keeping the MIT notice |
| `wordpress-theme` (tuleva.ee) | No licence file | ✅ **Copy, under Tuleva's authorisation.** Add a short attribution note where copied (e.g. `// adapted from TulevaEE/wordpress-theme with Tuleva's permission`), because our repo is public |
| Tuleva logo / brand | No public policy | ✅ Authorised for this project |
| Roboto, Merriweather | OFL | ✅ |

## Calculator UX to reuse

The homepage fee calculator is at `/pensionikalkulaator/`. Source: `wordpress-theme` (`src/wp-content/themes/tuleva/`) `templates/components/front-hero/calculator.php`, `js/calculator.js`, `scss/_custom-slider.scss`. **We can port it directly to React.**

- **Card:** a Bootstrap `.card.calculator.rounded-4` with a soft layered shadow (`--box-shadow-calculator`) and a light-blue inner panel (`bg-blue-2`).
- **Inputs:**
  - each row has the label on the left and the field on the right;
  - number fields carry a unit suffix ("€/kuu");
  - discrete options are segmented `btn-check` radio buttons;
  - continuous assumptions use a `form-range` slider with a floating white tooltip showing the value and a reference marker under the track (e.g. "7% historical return").
- **Output:** two rows of bold `fs-5` euro totals, comparing "the typical fund" against "Tuleva". The difference is phrased **"Maksad X € rohkem"** ("you pay X € more"), in red when it's bad. There is no chart.
- **Tone:** second person, plain, numbers first, slightly provocative. The CTA is a short first-person `btn-primary btn-lg` ("Too II sammas Tulevasse").
- **Logged-in React calculators** (MIT, reusable) in `onboarding-client`:
  - `src/components/millionaire/MillionaireCalculator.tsx` (+ `calculation.ts`, `prefill.ts`): a Chart.js two-line comparison, "Tänasel kursil" in `#006ce6` vs "Tuleva retseptiga" in `#008300`. The gap copy reads "koguksid X € rohkem". Below it is a "Sinu järgmised sammud" checklist.
  - `src/components/pensionCalculator/PensionCalculator.tsx`: stat tiles.
- **Mapping to our calculator:**
  - **Inputs:** balance and term as number fields with suffixes; the current margin as a number field or slider, with an "Eesti keskmine ~1.7%" marker.
  - **Comparison rows:** "Praegu maksad" (what you pay now) vs "Ühispakkumisega" (with the joint offer: worst / expected / best).
  - **Difference:** "Säästaksid X €" (you'd save X €), in green.
  - **After validation:** the same card, with "✓ lepingust" (from your contract) badges on the validated inputs.

## Design tokens (from `onboarding-client/src/components/_variables.scss`)

| Token | Value |
|---|---|
| Navy (headings) | `#002F63` |
| Brand bright blue | `#00AEEA` |
| Primary / link blue (`$blue`) | `#006CE6` |
| Green / green-light | `#008300` / `#00A100` |
| Red / red-light | `#DB2200` / `#FF4614` |
| Yellow | `#FCE228` |
| Note box (bg / border) | `#FFF6D9` / `#FAEED0` |
| Text (primary / secondary / tertiary) | `#293036` / `#6B7074` / `#8A8D91` |
| Gray backgrounds | `#FAFBFC`, `#F5F7F9` |
| Gray borders | `#E0E6EC`, `#D6DEE6`, `#C8D0D8` |
| Blue backgrounds | `#F2FAFE`, `#EDF7FD` |
| Blue borders | `#D3E8F5`, `#C9E0EE`, `#BAD2E4` |
| Fonts | **Roboto** (body, self-hosted variable TTF in the client); **Merriweather** for display numbers and counters |
| Spacing | `$spacer` 1rem; extra steps 5 = 3rem, 6 = 6rem; gutter and container padding 2rem; container max 960px (client) |
| Radii | .25–1.5rem; cards .5rem; calculator `rounded-4` |
| Buttons | `$btn-padding-x: 1em`, weight 450, nowrap; `.btn-light` is white with blue text |

Logos: Tuleva's are in `wordpress-theme` `img/tuleva-logo.svg` and `onboarding-client/src/components/common/logo.svg`. The Smart-ID, Mobile-ID and ID-card logos are also in the theme's `img/`. Those are SK's trademarks, so use them as SK's brand guidelines allow.

## Counter and tracker precedents

- **Public stats API** (no auth): `GET https://onboarding-service.tuleva.ee/v1/statistics/investor-count` returns `{"count":86787}`. Code: `statistics/InvestorStatisticsController.java`.
  - It's a good template for our public, aggregate-only `/v1/pool/stats`.
  - The WordPress side caches it for a day and sanity-checks the range.
- **Member count UI:** a big `display-5` number with one line of copy: "Eesti inimest kogub Tuleva indeksfondides" ("Estonians save in Tuleva index funds").
- **Countdown (reusable):** navy flip-style digit tiles in Merriweather (days, hours, minutes), used for the fund-switch deadline (`counter-hero.php`, `countdown-timer.php`, `_counter.scss`). **This is a direct visual precedent for our pool deadline.**
- **No progress-bar component exists.** We build our own with Bootstrap `.progress` and milestone markers.

## Auth and signing (`onboarding-service`, MIT)

**Libraries** (`build.gradle.kts`):

| Library | Version |
|---|---|
| `ee.sk.smartid:smart-id-java-client` | 3.2 (RP API v3) |
| `ee.sk.mid:mid-rest-java-client` | 1.7 |
| `eu.webeid.security:authtoken-validation` | 3.2.1 |
| `org.digidoc4j:digidoc4j` | 6.2.0 |
| BouncyCastle | 1.86 |

The service runs on Spring Boot 4.1.1 and Java 25.

**Demo config** (`application.yml`):
- **Smart-ID:**
  - RP UUID `00000000-0000-4000-8000-000000000000`, name `DEMO`
  - URL `https://sid.demo.sk.ee/smart-id-rp/v3/`
  - CA certificates in `classpath:smart-id/demo/*.pem`
- **Mobile-ID:**
  - RP UUID `00000000-0000-0000-0000-000000000000`, name `DEMO`
  - Host `https://tsp.demo.sk.ee/mid-api`
  - Truststore `test_keys/truststore.jks`

**Login flows:**
- **Smart-ID:** the anonymous device-link / QR flow (`POST /v1/smart-id/login`, `GET …/qr-code`, `POST …/callback`).
  - ⚠️ **The callback URL must be https.** For a local demo, use the QR (cross-device) flow, or the notification flow by personal code, or tunnel through https.
- **Mobile-ID:** `POST /authenticate`.
- **ID-card:** Web eID.
- Sessions are JWTs (`auth/jwt`, `POST /oauth/token`).

**Signing is fully supported.** It lives in the `ee.tuleva.onboarding.signature` package:
- `SignatureService` and `DigiDocFacade` build the ASiC-E container;
- `SmartIdSigner`, `MobileIdSigner` and `IdCardSigner` do the signing.

Endpoints follow the generic `SignatureController<T>` pattern:

```
PUT  /{id}/signature/smart-id      → GET …/smart-id/status
PUT  /{id}/signature/mobile-id     → GET …/mobile-id/status
PUT  /{id}/signature/id-card       → PUT …/id-card/signature → status
```

The reference implementation is `mandate/MandateController`. That fits our "commitment" document well: a pension switch mandate is conceptually the same as our conditional switching commitment.

**Minimal extraction set:**
- packages: `auth/smartid`, `auth/mobileid`, `auth/webeid`, `auth/ocsp`, `auth/jwt`, `signature/**`;
- config: `config/SmartIdClientConfiguration`, `SmartIdTrustedCaCertificates`, `MobileIdConfiguration`, `DigiDocConfiguration`;
- resources: `smart-id/demo/*.pem`, `test_keys/truststore.jks`.

**Demo test accounts** (official SK documentation):
- **Smart-ID** ([test accounts](https://sk-eid.github.io/smart-id-documentation/test_accounts.html)):
  - device-link OK: `PNOEE-40404040009-MOCK-Q`
  - notification OK: `40504040001` (`PNOEE-40504040001-DEM2-Q`), `50001029996`, `39901012239`
  - USER_REFUSED: `30403039917`; TIMEOUT: `30403039983`
- **Mobile-ID** ([test numbers](https://github.com/SK-EID/MID/wiki/Test-number-for-automated-testing-in-DEMO)):
  - OK: `+37268000769` / `60001017869`; also `+37200000766` / `60001019906` (7 s delay)
  - NOT_MID_CLIENT: `+37200000266`; USER_CANCELLED: `+37201100266`

## Repo conventions (from their CLAUDE.md / AGENTS.md)

- **onboarding-service:**
  - Java 25 (preview), Gradle Kotlin DSL, Spring Boot 4, Spring Modulith (each top-level package is a module).
  - Runs with `./gradlew bootRun` on :9000, with Postgres via docker compose.
  - Tests: JUnit 5 + **Spock** (Groovy) with AssertJ and BDDMockito; test slices are preferred. Strict TDD.
  - Spotless (Google Java style), Flyway with strict ordering, NullAway `@NullMarked`.
  - No comments or Javadoc. Inject `Clock`.
  - A **`.githooks/pii-scan`** hook blocks commits containing PII. **We should adopt something similar**, because our repo is public.
- **onboarding-client:**
  - CRA (react-scripts 5), React 17, TS 4.9, Bootstrap 5.3 + react-bootstrap, Redux, react-query 4, react-intl, react-router 5, Chart.js 4.
  - Node 22. Jest + RTL + msw. eslint (`@transferwise`) + prettier, husky.
  - Code lives in `src/components/<feature>/`.

**Recommendation for our new app:**
- **Frontend:**
  - Keep Tuleva's *libraries and look*: Bootstrap 5.3 with their SCSS variables, react-intl with Estonian only, react-hook-form, react-query, Chart.js.
  - Scaffold it with **Vite** (React 19, TS 6 as of the template), not CRA, which is deprecated and slow.
  - Keep the feature-folder structure so components can be ported later.
- **Backend:**
  - Spring Boot 4 / Java 25 / Gradle KTS, copying the auth and signature packages above.
  - For tests, keep JUnit 5 and decide whether Spock is worth it in the timebox.

## Sources
- https://github.com/TulevaEE/onboarding-service (MIT)
- https://github.com/TulevaEE/onboarding-client (MIT)
- https://github.com/TulevaEE/wordpress-theme (no licence)
- https://onboarding-service.tuleva.ee/v1/statistics/investor-count
- https://sk-eid.github.io/smart-id-documentation/test_accounts.html
- https://sk-eid.github.io/smart-id-documentation/environments.html
- https://github.com/SK-EID/MID/wiki/Test-number-for-automated-testing-in-DEMO
