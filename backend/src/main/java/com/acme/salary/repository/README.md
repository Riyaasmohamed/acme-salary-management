# `repository` — talking to the database

## `EmployeeRepository`

**What it does:** a Spring Data JPA interface. Spring generates the implementation at
start-up; we only declare methods.

**Responsibility:** every database read and write for employees:

| Method | SQL it becomes | Used for |
|---|---|---|
| `findAll(Specification, Pageable)` (inherited) | `SELECT … WHERE … ORDER BY … LIMIT/OFFSET` + `COUNT(*)` | the paged employee list |
| `findById`, `save`, `saveAndFlush`, `deleteById`, `existsById` (inherited) | single-row operations | CRUD |
| `existsByEmail`, `existsByEmailAndIdNot` | `SELECT … WHERE email = ? [AND id <> ?]` | unique-email check on create / update |
| `salaryStatsByCountry()` | `MIN, MAX, AVG, COUNT … GROUP BY country, currency` | insights |
| `salaryStatsByJobTitle(country)` | same, `WHERE country = ? GROUP BY job_title, currency` | insights |
| `headcountByCountry()` | `COUNT(*) GROUP BY country` | insights |
| `findDistinctCountries/JobTitles/Departments()` | `SELECT DISTINCT … ORDER BY …` | filter dropdowns |

**Why it exists:** keeps all SQL in one place. The aggregate queries are the reason the app
never loads 10,000 employees into memory: the database computes the statistics and returns a
handful of rows.

The aggregate queries use a JPQL *constructor expression*
(`select new com.acme.salary.repository.SalaryStatsRow(...)`), which tells Hibernate to put
each result row straight into a small record.

## `EmployeeSpecifications`

**What it does:** builds the `WHERE` clause for the list from four optional inputs: search
text, country, job title and department.

**Responsibility:** combine only the criteria that are filled in, with `AND`. The search
matches the full name ("asha rao") and the email, case-insensitively. Special LIKE characters
(`%`, `_`) typed by the user are escaped so they are searched literally.

**Why it exists:** four optional filters make 16 possible queries. Writing them as separate
repository methods, or building SQL strings by hand, would be error-prone. A specification
adds one predicate per filled-in criterion.

## `SalaryStatsRow` and `HeadcountRow`

**What they do:** tiny records that hold one row of an aggregate query
(`SalaryStatsRow`: group key, currency, min, max, average, count).

**Why they exist:** the repository returns database-shaped data; the service turns it into
the API shape (for example, it rounds the average). This keeps the repository independent of
the API's JSON format. `SalaryStatsRow` is shared by the "by country" and "by job title"
queries because their columns are identical — only the grouped column differs.
