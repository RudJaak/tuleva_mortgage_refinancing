# Synthetic documents

Fake but realistic Estonian documents for demos and tests (TASKS.md T50). Every page is marked
**NÄIDIS** and the lender is fictional unless you override it.

## Loan contract

```bash
node tools/synthetic-documents/generate-contract.mjs --name "Mari Maasikas" [options]
```

This writes `<name>-laenuleping.pdf` and `<name>-standardinfo-teabeleht.pdf` (the ESIS) to `--out`
(default: the current directory). It needs a Chromium-based browser (Chrome, Brave, Chromium or
Edge), or set `BROWSER` to its executable.

| Option | Default | Notes |
|---|---|---|
| `--name` | (required) | Borrower's full name; the demo matches it to the logged-in person |
| `--code` | none | Borrower's personal code, printed next to the name |
| `--co-borrower` | none | A second borrower, to demo the co-borrower flag |
| `--lender` | `AS Näidispank` | Keep it fictional in anything shared |
| `--number` | `NÄIDIS-2019-0614` | Contract number |
| `--date` | `2019-06-14` | Signing date |
| `--principal` | `185000` | Loan amount in euros |
| `--margin` | `1.95` | Margin over 6M Euribor, percent |
| `--months` | `360` | Term |
| `--payment-day` | `14` | Day of the month |
| `--property-value` | `231000` | In the ESIS, for the LTV estimate |
| `--address` | `Harju maakond, Tallinn, Näidise tn 1-12` | The mortgaged property |
| `--fixed-rate`, `--fixed-until` | none | A running fixed-rate period, to demo that flag |
| `--out` | `.` | Output directory |

The instalment, APR, maturity and mortgage amount are calculated (Euribor taken as 0 at signing).

**For a demo in your own name**, generate into the repo root (`/*.pdf` is gitignored), sign both
PDFs together in DigiDoc4 (it makes one `.asice`), and upload that on the loan step. Never commit
a document with a real person's name or code; `fixtures/` holds only fictional people.
