# `service` — business logic

Services sit between the controllers (HTTP) and the repository (database). They hold the
rules of the application and define the transaction boundaries.

## `EmployeeService`

**What it does:** list, get, create, update and delete employees, and provide the values for
the filter dropdowns.

**Responsibility:** the business rules for employee data:

- **Email must be unique.** Before saving, it checks `existsByEmail` (create) or
  `existsByEmailAndIdNot` (update, so an employee can keep their own email) and throws
  `DuplicateEmailException` → HTTP 409.
- **Unknown ids** throw `EmployeeNotFoundException` → HTTP 404.
- **Transactions:** the class is `@Transactional(readOnly = true)`; methods that write
  override it with `@Transactional`. Read-only transactions let Hibernate skip dirty checks.
- `update` uses `saveAndFlush` so the `updatedAt` timestamp in the response is the new one.

**Why it exists:** controllers stay thin (HTTP only) and the rules can be unit-tested with
Mockito without starting Spring or a database (`EmployeeServiceTest`).

## `InsightsService`

**What it does:** answers the three HR questions by calling the aggregate queries.

**Responsibility:** turn `SalaryStatsRow` / `HeadcountRow` into the API DTOs and **round the
average** to two decimals (`HALF_UP`). The database returns `AVG` as a floating-point number;
money is shown with two decimals.

**Why it exists:** keeps the reporting logic separate from the employee CRUD logic. It does
no calculation in Java loops — all aggregation happens in SQL.
