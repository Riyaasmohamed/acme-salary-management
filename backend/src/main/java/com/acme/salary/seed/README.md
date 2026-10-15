# `seed` — 10,000 demo employees

## `EmployeeDataGenerator`

**What it does:** creates realistic employees in memory (no database access):

- **8 countries**, each with its own currency and a "pay factor" that turns a USD-based
  salary into a realistic local amount (India 25%, United States 22%, United Kingdom 12%,
  Germany 10%, Canada 9%, Australia 8%, Singapore 7%, Japan 7% of staff),
- **18 job titles** in **10 departments**, each with a base salary,
- salary = base × country factor × random spread (0.85–1.25), rounded to the nearest 100,
- hire dates spread over the last 15 years, never in the future,
- unique emails (`asha.rao@acme.com`, then `asha.rao2@acme.com`, …).

**Why it is separate from the seeder:** it is a pure function of `(seed, count, today)`, so
it is easy to test (`EmployeeDataGeneratorTest` checks count, unique emails, one currency per
country, and that the same seed gives the same data).

## `EmployeeSeeder`

**What it does:** an `ApplicationRunner`, so Spring calls it once after start-up. If seeding
is enabled and the employee table is **empty**, it generates the employees and saves them
with `saveAllAndFlush` in **one transaction** (inserts are batched 500 at a time; about 2–3 seconds).

**Why it works this way:**

- **Only when empty:** restarting the app never overwrites the HR Manager's changes.
- **One transaction:** all 10,000 rows appear at once; if anything fails, nothing is saved
  and the next start tries again.
- **Deterministic:** the fixed random seed means everyone sees the same demo data.

To start again from fresh data: stop the backend, delete `backend/data/`, start it again.
