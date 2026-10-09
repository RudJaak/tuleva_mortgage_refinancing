# Research: Tuleva context

_Researched 2026-10-09._

## Organisation
- **Tulundusühistu Tuleva** is a member-owned cooperative (one member, one vote). It owns 100% of **Tuleva Fondid AS**, the fund manager.
- Funds: II pillar (Maailma Aktsiad, Maailma Võlakirjad), III pillar, and Täiendav Kogumisfond.
- About **86.8k savers** (tuleva.ee homepage). AUM is about **€1.8bn** (public API `onboarding-service.tuleva.ee/v1/funds`).
- Brand tone: plain-spoken Estonian, numbers-forward, low-fee, member-owned. Calls to action are short and in the first person ("Toon II samba Tulevasse").

## Hackathon
- **9–11 Oct 2026** at Telliskivi Loomelinnak, Roheline saal. The winner gets budget and support to turn the idea into a product.
- The challenge themes in Tuleva's code are `FAIR_LENDING`, `INSURANCE`, `COLLECTIVE_BUYING_POWER` and `WEALTH_AND_INHERITANCE`.
- The registration email says challenges "are not offers of Tuleva products or services". This affects how the pitch can be worded.
- Related ideas on the list:
  - **L4**: each member compares bank offers individually.
  - **P3**: joint procurement of home insurance. No evidence was found that Tuleva has actually run this.

## Tech stack (GitHub: TulevaEE)
- **onboarding-service**: Java 25, Spring Boot 4.1, Spring Modulith, PostgreSQL with Flyway, AWS ECS Fargate (eu-central-1).
  - Auth: Smart-ID, Mobile-ID and ID-card (web-eid).
  - Integrations: EPIS (Pensionikeskus), the SEB gateway, Montonio, Äriregister and OpenSanctions.
  - Modules include `populationregister`, `kyc`, `aml` and a **`hackathon`** package.
- **onboarding-client**: React 17, TypeScript 4.9, Redux, react-hook-form, react-intl, Bootstrap 5, `@web-eid/web-eid-library`.
- Both repos contain CLAUDE.md / AGENTS.md files, which are worth reading before matching conventions.

## Saver statistics (private)

Tuleva provided aggregated internal saver statistics: salary bands, age and assets. The digest is in `docs/inputs/saver-stats-notes.md`, which is gitignored because the repo is public. **Do not copy those figures into committed files.**

Takeaways that are safe to repeat:
- Tuleva can see the salary of savers whose II-pillar contributions flow to it, because contributions are a fixed % of gross pay. With consent, it could **pre-fill or verify gross employment income**; the legal basis is to be checked.
- Tuleva's saver base is concentrated in typical mortgage-holder ages. A meaningful prime-quality segment exists to seed the first pool.
- Tuleva has **no mortgage data**.

## Sources
- https://tuleva.ee/
- https://onboarding-service.tuleva.ee/v1/funds
- https://github.com/TulevaEE/onboarding-service
- https://github.com/TulevaEE/onboarding-client
- Internal (private, not committed): `docs/inputs/`
