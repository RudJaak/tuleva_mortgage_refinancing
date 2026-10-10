# frontend/CLAUDE.md

React app for the borrower flow. Project context and decisions live in `../CLAUDE.md`, `../PLAN.md` and `../TASKS.md`.

## Stack

- Vite 8, React 19, TypeScript 6.
- Bootstrap 5.3 with react-bootstrap.
- react-intl, react-router 7, TanStack Query 5, react-hook-form.
- Tests: Vitest + Testing Library (jsdom). Lint: oxlint. Format: Prettier (no semicolons, single quotes, width 100).
- Dev server on **:5173**, also reachable from the local network (`host: true`, for demoing on phones). It proxies `/v1` and `/actuator` to the backend on :9000, so the app always calls relative URLs through `src/api/client.ts`.

## Design: Tuleva look and feel (reuse authorised, D12)

- Styles live in `src/styles/`, copied from TulevaEE/onboarding-client (MIT): `_variables.scss`, `_base.scss`, `_high-contrast.scss` and the Roboto variable font.
  - **Build on Bootstrap utilities and these variables. Don't invent new colours.**
  - Key colours: navy `#002F63` for headings (`.text-navy`), primary blue `#006CE6`, green `#008300` for savings, red `#DB2200`.
- Merriweather comes from Google Fonts in `index.html` and is used for big display numbers such as counters.
- Calculator and countdown visuals should be ported from TulevaEE/wordpress-theme. See `../docs/research/08-tuleva-design-and-code.md` for file paths. Add a one-line attribution comment wherever Tuleva code is copied.
- Feature SCSS partials sit next to their feature (e.g. `components/flow/_stepper.scss`) and are imported from `src/styles/index.scss`.
- Check every screen at **1280 px and 390 px**.

## Structure

```
src/
  main.tsx                  entry: providers + BrowserRouter
  components/
    AppProviders.tsx        IntlProvider (et), QueryClientProvider
    AppRoutes.tsx           all routes; paths.ts holds the Estonian URL slugs and flow steps
    <feature>/              landing, login, flow, done, layout, common
  translations/translations.et.json
  api/client.ts             fetch wrapper (credentials included, JSON)
  styles/
  test/setup.ts
```

## Conventions

- **The UI is Estonian only (D11).**
  - Every user-visible string goes in `translations.et.json` and is rendered with `FormattedMessage` / `useIntl`. No hard-coded copy.
  - Keys are dotted by feature (`flow.loan.heading`).
- **Landing-page persuasion content** (facts, statistics, slogans from teammates) comes from a JSON content file (T14), not from translations.
- **Compliance wording** (Reklaamiseadus § 29): show margins as goals ("Eesmärk"), never as offers ("Pakkumine"). Pair any margin figure with a disclaimer.
- **Code:** function components with named exports, and colocated tests (`*.test.tsx`). Query with `getByRole` and Estonian accessible names. Write the test first where practical.
- **Data fetching:** use TanStack Query hooks per feature. **Keep financial maths in a single tested module** (`src/calc/`, T11) and never duplicate it in components.
- **Personal data:** never log or persist personal codes or documents client-side. `sessionStorage` is allowed only for anonymous calculator inputs.

## Commands

```bash
npm install
npm run dev            # http://localhost:5173 (start the backend for /v1 calls)
npm test               # vitest run
npm run lint && npx tsc -b && npm run format:check
npm run build
```
