# Digest: teammate's "Commercial Assessment of Residential Mortgage Refinancing in Estonia"

_Source: `README.pdf`, 18 pages, added 2026-10-09. A teammate's AI agent wrote it. This digest pulls out the parts we need, so the PDF can be deleted._

> **Different framing from ours.** The report asks how a *new lender* (a foreign bank, a branch, or a white-label partner) could take refinancing share. L6 asks how *Tuleva* could pool members and run a *joint tender*. The market numbers apply to both. Treat the entry-model conclusions as background only.

> **Claim check is done.** See the bottom of `01-market.md`.
> - Contract count refuted: the official figure is about 192k households.
> - Deposit rates refuted: about 2.0–2.4%.
> - Stock margin plausible but unsourced.
> - Harjumaa share and notary reform timing unverified.

## Reliability warning

Many key figures cite source "3. unknown_url", and the report itself calls them "mathematically estimated" or "modelled". Use them for orders of magnitude, **not** as facts:
- 172k contracts and 158k households
- the legacy-stock margins of 1.85–2.20%
- the loan-size distribution table
- the SAM/SOM figures and the "70% of principal has more than 15 years remaining" figure
- the Harjumaa share of principal (70–75%), which is extrapolated

Also suspect: the claim that Estonian term deposits pay 3.2–4.5%. Check it against Eesti Pank before using.

Better-sourced claims: the €12.8bn stock (Q3 2025, via kinnisvarauudised and Eesti Pank), new-loan margins of about 1.50% in April 2024 (Eesti Pank press release), the macroprudential limits, and the Maa-amet price figures.

## Key numbers

| Metric | Value | Confidence |
|---|---|---|
| Outstanding housing loans | **€12.8bn** (Q3 2025), about 90% of household lending | Good |
| Active contracts / households | ~172k / ~158k | Estimate |
| Average margin on new loans | ~1.50% (April 2024) | Good (Eesti Pank) |
| Margin on legacy stock (2015–2022 vintages) | 1.85–2.20% | Estimate. **This is the core arbitrage claim; verify it.** |
| Achievable margin for a well-funded lender | 1.30–1.45% | Opinion |
| Realistic margin reduction | 0.50–0.75 pp | Opinion |
| House prices 2010–2024 | +228% (Eurostat) | Good |
| Tallinn new / secondary apartments, €/m² (Q1 2025) | €4,126 / €2,798. Outside Tallinn, secondary: €1,320 | Good (Maa-amet) |
| Notary + state fee to transfer a mortgage | ~€400–500 | Moderate |
| Early-repayment penalty | 3 months' interest unless 3 months' notice is given (Võlaõigusseadus) | Good |

## Loan-size distribution (modelled)

| Balance | Share of contracts | Share of principal |
|---|---|---|
| < €50k | 30% | 11.5% |
| €50–75k | 20% | 16% |
| **€75–200k** | **43%** | **62%** |
| > €200k | 7% | 10.5% |

**Takeaway:** fixed switching costs make loans below about €50–75k marginal. The best value is in **€75–200k balances with more than 15 years remaining**, originated in **2018–2022**, mostly in **Harjumaa**.

## Savings for a household

Example: €125k balance, 20 years remaining, 2.5% Euribor, current margin 1.85%, €400 switching cost, NPV discounted at 3%.

| Margin cut | Saving per month | Saving over the loan's life | NPV | Break-even |
|---|---|---|---|---|
| 0.25 pp | €17 | €4.0k | €2.6k | 24 months |
| **0.50 pp** | **€33** | **€7.9k** | **€5.6k** | **12 months** |
| 0.75 pp | €49 | €11.8k | €8.5k | 8 months |
| 1.00 pp | €65 | €15.7k | €11.4k | 6 months |

For €250k over 25 years, a 0.50 pp cut saves more than €70 a month (NPV above €12k). **The pitch should lead with the lifetime and NPV figures.** "€33 a month" alone sounds small.

Formula used (standard annuity): `PMT = P · r(1+r)^n / ((1+r)^n − 1)`, with monthly rate r and n months. The annuity is recalculated at every Euribor reset, typically every 6 months.

## Market sizing (modelled)

- **TAM:** €12.8bn, about 172k contracts.
- **SAM:** balance over €75k, more than 10 years remaining, and credit-eligible. That leaves **~75–85k contracts and €7.8–9.3bn**, about 80% of it in Harjumaa.
- **SOM, base case:** 4.5k households in 3 years, 11.25k in 5 years (≈€1.17bn, ≈€4.5M a year in household savings), 22.5k in 10 years.

## Risks relevant to L6

1. **Incumbents retaliate. This is the main risk.** When a borrower asks for a payoff quote, the current bank's retention desk can simply match the offer. The entrant then becomes "an unpaid price-discovery mechanism".
   - *For Tuleva this cuts both ways.* If the mission is lower margins for members, an incumbent matching the offer is still a win for the member.
   - It does weaken a pool's bargaining power with banks, though, unless members commit.
   - **Design question:** how binding is joining the pool?
2. **The notary-fee reform** (bill 864 SE) is not in force yet. Until it is, the roughly €400 fixed cost hurts conversion.
3. **The 90-day notice:** the switching flow should send the incumbent bank the early-repayment notice as soon as pre-approval arrives, so the switch happens on day 91 with no penalty.
4. **There is no loan-level data on margins and maturities.** The report recommends a survey or anonymised data from a credit bureau. **Our data-collection app is exactly that survey.**

## Recommended target segment

Harjumaa, 2018–2022 vintage, €100–200k balance, 15+ years remaining, margin at or above ~1.85%.
