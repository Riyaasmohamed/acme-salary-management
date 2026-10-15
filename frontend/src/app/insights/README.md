# `insights` — answering questions about pay

## `insights.model.ts`

Types for the three insight responses: `CountrySalaryStats`, `JobTitleSalaryStats`,
`CountryHeadcount`.

## `insights.service.ts` — `InsightsService`

**What it does:** calls `GET /api/insights/salary-by-country`, `…/salary-by-job-title?country=`
and `…/headcount-by-country`. All numbers are computed by the database; the UI only formats
them.

## `insights-page/` — `InsightsPage` (route `/insights`)

**What it shows:**

1. **Summary cards** — total employees, number of countries, the largest team.
2. **Salary range by country** — a table with minimum, average and maximum annual salary,
   each in that country's currency.
3. **Headcount by country** — a bar chart, largest first.
4. **Average salary by job title** — pick a country (defaults to the largest); shows a bar
   chart of averages and a table with employees / min / average / max per title.

**How it works:**

- The overview (2 and 3) is loaded with `forkJoin`, so both requests run in parallel and the
  page renders once both have answered.
- The job-title section reacts to the selected-country signal with `switchMap`; switching
  countries quickly never shows stale data.
- Loading, empty and error (with "Try again") states.
- Money is formatted with the currency **code** (e.g. `INR 1,830,243`) because the table
  mixes currencies and symbols like `$` are ambiguous (USD, CAD, AUD, SGD).
- Salaries are never added up or averaged across currencies — that is why there is no
  "global average salary" card.

## Tests

`insights.service.spec.ts`.
