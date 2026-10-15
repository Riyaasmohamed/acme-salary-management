# Performance at 10,000 employees

The goal was "fast enough that the HR Manager never waits", achieved with simple, standard
techniques rather than caching or extra infrastructure. All numbers below were measured on
the development machine (16 cores, Java 17, H2 file database, 10,000 seeded employees).

## The four techniques

| Technique | Where | Effect |
|---|---|---|
| **Server-side pagination** (max 100 rows per request) | `EmployeeController`, `EmployeeService` | The browser receives ~6 KB per page instead of ~3 MB for all employees. Response size does not grow with the table. |
| **Aggregates in SQL** (`GROUP BY` with `MIN/MAX/AVG/COUNT`) | `EmployeeRepository` | Insight endpoints return 8–18 rows (~1 KB). No employee entity is loaded into Java memory. |
| **Indexes** on `country`, `job_title`, `department`; unique index on `email` | `V1__create_employee_table.sql` | Filters and the uniqueness check read only matching rows (see measurements). |
| **Batched inserts** (sequence ids, `batch_size=500`) | `Employee`, `application.properties` | The 10,000-row seed takes **2–3 seconds**. |

## Measured API response times

30 requests per endpoint after warm-up, measured with `curl` against the running backend
(includes HTTP, JSON serialisation and the database):

| Endpoint | Median | p95 |
|---|---|---|
| `GET /api/employees?page=0&size=20` | 7.5 ms | 16.1 ms |
| `GET /api/employees?page=400&size=20&sortBy=SALARY&direction=DESC` (deep page) | 7.7 ms | 12.3 ms |
| `GET /api/employees?search=patel` (contains-search) | 28.2 ms | 47.0 ms |
| `GET /api/employees?country=India&jobTitle=Data Analyst` | 5.8 ms | 9.6 ms |
| `GET /api/employees/filter-options` | 3.1 ms | 6.1 ms |
| `GET /api/insights/salary-by-country` | 4.9 ms | 6.6 ms |
| `GET /api/insights/salary-by-job-title?country=India` | 6.2 ms | 12.4 ms |
| `GET /api/insights/headcount-by-country` | 3.6 ms | 6.3 ms |

Timings vary between runs on the same machine: an earlier run measured roughly twice these
values (for example 12 ms for the first page and 56 ms for the search). Treat them as orders of
magnitude — every request is well under the ~100 ms at which a user starts to notice a delay.

Response sizes: one page of 20 employees is 6.1 KB, 100 employees 30 KB, salary-by-country
1 KB.

## Before / after: what the indexes change

Measured with H2 `EXPLAIN ANALYZE` on a copy of the seeded database. `scanCount` is the
number of rows the database looked at; `reads` is the number of storage pages read.

| Query | With index | Without index |
|---|---|---|
| Employees in one country (`WHERE country = 'India'`) | index `IDX_EMPLOYEE_COUNTRY`, **2,480 rows, 107 page reads** | table scan, **10,001 rows, 1,731 page reads** (16× more reads) |
| Salary stats per job title in a country | index on country, 2,480 rows, 1,534 reads | table scan, 10,001 rows, 1,731 reads |
| Email uniqueness check (`WHERE email = ?`) | unique index, **1 row, 2 reads** | — (the unique constraint always creates this index) |

Two queries intentionally read the whole table, and that is correct:

- **Salary by country** must look at every employee to compute every country's statistics
  (10,001 rows, 4 ms end to end). An index cannot reduce the work of a full aggregate.
- **Contains-search** (`LIKE '%patel%'` on name and email) cannot use a normal B-tree index
  because the pattern starts with a wildcard. At 10,000 rows this is the slowest endpoint
  (about 30–60 ms median), still well below what a user notices.

## Why not load everything and compute in Java?

Loading 10,000 entities to compute an average would transfer every row from the database,
create 10,000 objects and keep them in memory for every dashboard request. The SQL version
transfers 8 rows. The same applies to the list: filtering or sorting in the browser would
require downloading all employees first.

## What would change at 100,000+ employees

Not needed now, listed so the trade-offs are visible:

1. **Search**: a full-text or trigram index (PostgreSQL `pg_trgm`) or a prefix-only search
   (`LIKE 'pat%'`, which can use an index).
2. **Deep pages**: keyset ("seek") pagination (`WHERE (salary, id) < (?, ?)`) instead of
   `OFFSET`, which gets slower the deeper the page.
3. **Count query**: every page request also runs `COUNT(*)`; for very large tables the UI
   could show "more than N" instead of an exact total.
4. **Composite index** `(country, job_title)` so the per-job-title statistics read only the
   index for one country.
5. **Insights**: cache the aggregate results for a short time or maintain a summary table,
   because salary data changes rarely compared with how often it is read.
6. **Database**: PostgreSQL instead of H2 for concurrent users and proper production
   operations.
