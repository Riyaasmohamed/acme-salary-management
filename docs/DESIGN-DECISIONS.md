# Design decisions and trade-offs

Each decision lists what was chosen, why, and what it costs. The guiding rule was
**the simplest thing that is correct at 10,000 employees and easy to explain**.

## Backend

### 1. Plain layered architecture (controller → service → repository)
- **Why:** the domain is one entity with CRUD plus reporting. Layers give clear
  responsibilities without extra abstractions.
- **Trade-off:** hexagonal/ports-and-adapters would isolate the domain from Spring more
  strictly, but it would double the number of types for no benefit at this size.

### 2. DTOs (Java records) instead of exposing the JPA entity
- **Why:** the JSON contract stays stable when the table changes; clients can never send
  `id`, `createdAt` or `updatedAt`; validation rules live on the request type, not on the
  persistence model; no lazy-loading or serialisation surprises.
- **Trade-off:** a mapper and a few extra classes.

### 3. Hand-written static mapper instead of MapStruct
- **Why:** one entity with nine fields. A short class that anyone can read beats a code
  generator plus annotations to explain. It is also the one place where input is normalised
  (trimmed text, lower-case email, upper-case currency).
- **Trade-off:** must be updated by hand when a field is added (the compiler and the tests
  catch it).

### 4. No Lombok
- **Why:** every getter, setter and constructor is visible in the code, which makes the
  classes easy to read in a review. Records already remove most boilerplate for DTOs.
- **Trade-off:** the entity is longer.

### 5. Flyway migration owns the schema; Hibernate only validates (`ddl-auto=validate`)
- **Why:** the schema, indexes and constraints are written explicitly in
  `V1__create_employee_table.sql`, are versioned, and are the same in every environment.
  `ddl-auto=update` can silently create or miss indexes and cannot be reviewed.
- **Trade-off:** schema changes need a new migration file.

### 6. H2 in file mode
- **Why:** zero installation (it is a Maven dependency), real SQL with indexes, `GROUP BY`
  and constraints, and data survives restarts. Tests use H2 in memory, so they run anywhere.
- **Trade-off:** single process, not for production. Moving to PostgreSQL means changing the
  JDBC URL and driver dependency; the JPQL queries and the migration are standard SQL.
- The H2 web console (`/h2-console`) is enabled so the data can be inspected locally with SQL;
  it only accepts connections from the same machine and would be disabled outside development.

### 7. `BigDecimal` for money, `DECIMAL(14,2)` in the database
- **Why:** `double` cannot represent amounts like 0.10 exactly; money must not drift.
  Averages come back from the database as a floating-point number and are rounded once,
  `HALF_UP` to two decimals, in `InsightsService`.
- **Trade-off:** slightly more verbose arithmetic.

### 8. Statistics grouped by country **and** currency; no currency conversion
- **Why:** averaging INR and USD salaries together would produce a meaningless number.
  Grouping by currency guarantees every number is in one real currency. Exchange rates
  change daily and need a trusted source, so conversion is out of scope
  (see [REQUIREMENTS.md](REQUIREMENTS.md)).
- **Trade-off:** no single "global average salary". The dashboard shows headcount totals
  instead, which are currency-independent.

### 9. Aggregates computed in SQL, never in Java loops
- **Why:** `MIN/MAX/AVG/COUNT … GROUP BY` returns 8 rows instead of transferring 10,000
  entities into memory; the database is built for this. It also scales: the API response
  size does not grow with the number of employees.
- **Trade-off:** JPQL constructor expressions (`select new …SalaryStatsRow(...)`) are a little
  less familiar than derived query methods. Each query is documented in the repository.

### 10. Server-side pagination with a hard page-size limit and a whitelisted sort
- **Why:** the UI never asks for more than 100 rows; `size` above 100 is rejected with 400.
  Sorting is limited to `NAME`, `SALARY`, `HIRE_DATE` (an enum), so clients cannot sort by
  arbitrary, unindexed or non-existent properties. `id` is added as a tie-breaker so rows
  with equal values do not jump between pages.
- **Trade-off:** offset pagination gets slower for very deep pages on huge tables (keyset
  pagination would fix that; not needed at 10,000 rows — see [PERFORMANCE.md](PERFORMANCE.md)).

### 11. Own `PageResponse` record instead of returning Spring's `Page`
- **Why:** Spring Data warns that `PageImpl`'s JSON is not a stable contract. The record has
  exactly the five fields the UI paginator needs.

### 12. JPA Specifications for the optional search and filters
- **Why:** four optional criteria combine into 16 possible WHERE clauses. A specification
  adds a predicate only for the criteria that are present, without string concatenation or
  `(:param is null or …)` tricks. LIKE wildcards in user input are escaped, so searching for
  `_` or `%` matches literally.
- **Trade-off:** the Criteria API is more verbose than JPQL; it is kept to one small class.

### 13. Unique email enforced three times, on purpose
1. **Normalisation**: `John@ACME.com ` is stored as `john@acme.com`.
2. **Service check** (`existsByEmail…`) gives a clear 409 with a message for the email field.
3. **Unique index** in the database is the real guarantee: if two requests race past the
   check, the second insert fails and is also mapped to 409.

### 14. Errors as RFC 9457 `ProblemDetail`
- **Why:** a standard, documented JSON error format built into Spring 6. Validation errors
  add an `errors` map (`field → message`) so the UI can show each message under its input.
  Extending `ResponseEntityExceptionHandler` means malformed JSON, wrong parameter types and
  unknown sort fields also return a proper 400 instead of a 500.
- Unexpected exceptions are logged and return a generic 500 without internal details.

### 15. Sequence-generated ids with `allocationSize = 50`
- **Why:** with `IDENTITY` ids Hibernate must insert rows one by one to learn each id, which
  disables JDBC batching. With a sequence it reserves 50 ids per round trip and sends inserts
  in batches of 500 (`hibernate.jdbc.batch_size`). The 10,000-row seed takes 2–3 seconds.

### 16. Deterministic seed, one transaction, only when the table is empty
- **Why:** a fixed random seed (42) produces the same employees every time, so demos and
  numbers are reproducible. One transaction means all 10,000 rows appear together (or none
  if something fails, and the next start retries). Skipping a non-empty table protects the
  HR Manager's edits across restarts.
- **Lesson from development:** the first version saved in 1,000-row chunks and the API could
  briefly show a half-seeded table; it was changed to a single transaction with
  `saveAllAndFlush` (see the commit history).
- **Trade-off:** hire dates are relative to the day the seed runs.

## Frontend

### 17. Standalone components, signals for state, RxJS for async work
- **Why:** this is the current recommended Angular style (no NgModules). Signals make page
  state explicit and synchronous to read; RxJS is still the right tool for debouncing and
  cancelling HTTP requests (`debounceTime`, `switchMap`).

### 18. The list's query is one signal
- **Why:** search, filters, sort and page are a single `EmployeeQuery` object. Any change
  produces a new object, which triggers exactly one request; `switchMap` drops the response
  of an older query if a newer one starts. Changing a filter or the sort always resets to
  page 0 so the user never lands on a page that no longer exists.

### 19. Validation in the UI mirrors the backend; the backend stays the source of truth
- **Why:** immediate feedback without a round trip for common mistakes. Anything the client
  cannot know (duplicate email) or misses is returned by the backend and shown on the same
  field.

### 20. Dates handled as local calendar days
- **Why:** `hireDate` is a `LocalDate` in Java and `YYYY-MM-DD` on the wire.
  `Date.toISOString()` converts to UTC and can move a date one day back for users east of
  UTC, so the UI formats the local day itself (`shared/dates.ts`, covered by tests).

### 21. No chart library
- **Why:** the dashboard needs horizontal bars only. A small CSS component is lighter, has
  no licence or bundle-size questions and is easy to test.

### 22. Dev-server proxy instead of CORS
- **Why:** the browser sees a single origin, so the backend needs no CORS configuration for
  local use. A production setup would serve the built UI from the same origin (or behind
  one reverse proxy) for the same reason.

## Testing

### 23. A test pyramid that stays fast
- **Unit tests** (JUnit 5 + Mockito) for services, mapper behaviour, the currency validator
  and the seed generator — no Spring context, milliseconds each.
- **Slice tests**: `@DataJpaTest` runs the real migration and the real SQL against in-memory
  H2 (aggregates, search, LIKE escaping, unique index); `@WebMvcTest` checks the HTTP
  contract (status codes, JSON, error bodies) with mocked services.
- **One `@SpringBootTest`** proves the whole context starts.
- **Angular**: services with `HttpTestingController`, components with `TestBed`, timing with
  `fakeAsync`.
- All tests use fixed data and fixed dates (no `now()` in assertions), so they are
  deterministic and order-independent.
