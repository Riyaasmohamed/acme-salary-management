# Requirements — ACME Salary Management

## Goal

Replace the Excel sheets ACME's HR team uses today with a web application where the
HR Manager can **maintain salary data for ~10,000 employees in several countries** and
**answer questions about how the organisation pays people**, without copying data
between spreadsheets.

## User persona

**HR Manager** — one trusted person (or a small HR team) who knows the data, works in a
desktop browser, and needs answers quickly. Not a technical user.

## Questions the HR Manager must be able to answer

1. What are the minimum, maximum and average salaries in each country?
2. What is the average salary for a given job title in a given country?
3. How many employees work in each country?

## In scope (what I am building)

| Feature | Why it matters to the HR Manager |
|---|---|
| Employee list with server-side pagination | 10,000 rows must stay fast; nobody scrolls through 10,000 rows |
| Search by name or email | The most common task is "find this person" |
| Filter by country, job title, department; sort by name, salary, hire date | Replaces Excel filters and sorting |
| Add, edit, delete an employee (with confirmation before delete) | Day-to-day maintenance of the data |
| Validation with clear messages (required fields, valid email, salary > 0, hire date not in the future, valid ISO currency, unique email) | Keeps the data trustworthy, which Excel does not |
| Insights page: salary stats per country, average salary per job title in a country, headcount per country | Answers the three questions above directly |
| Seed of 10,000 realistic employees in 8 countries | Lets the app be demonstrated and tested at real volume |

## Deliberately out of scope (and why)

| Left out | Reasoning |
|---|---|
| **Login / authentication / roles** | There is a single persona and the app runs locally for this exercise. Adding auth would cost time without helping answer the HR questions. In production this would be the first addition (SSO via the company identity provider). |
| **Payroll, tax, payslips** | The problem is *managing and analysing* salary data, not paying people. Payroll is a regulated, country-specific domain that deserves its own system. |
| **Currency conversion** | Exchange rates change daily and need an external source of truth. Converting would make numbers look comparable when they are not. Instead, statistics are always grouped **by country and currency**, so no average ever mixes INR with USD. |
| **Salary history / audit trail** | Valuable ("who changed this salary and when?") but it changes the data model (append-only history table) and the UI. Noted as the next feature after auth. |
| **Excel import/export** | Useful for migration, but the brief asks for managing data in the app. The seed covers demo data. Planned as a follow-up. |
| **Deployment / hosting** | This version runs locally (backend + frontend) by choice; see README for run steps. |

## Assumptions

- Each employee has exactly one current salary, stored as an **annual** amount in the
  currency of their contract.
- Email is the natural unique identifier of an employee (case-insensitive).
- Countries, job titles and departments are free text, so HR can add new ones; the
  filters show the values that exist in the data.

## Success criteria

- Listing, searching and filtering 10,000 employees responds in well under a second locally.
- All three questions are answered on one Insights page with numbers computed by the database.
- Invalid input is rejected with a message that tells the user exactly which field to fix.
- Core logic is covered by fast, deterministic tests.
