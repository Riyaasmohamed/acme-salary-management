# AI usage log

This is an honest record of how AI tools were used to build this project.

## Tool

**Claude Code** (Anthropic's agentic coding tool, running Claude Opus 5.5) in a terminal on
the development machine. It could read and write files, run shell commands (Maven, npm,
`curl`, git) and drive a headless browser. It did not deploy anything.

## How it was used

1. **Understanding the brief.** The assessment email and brief were pasted into the chat with
   the question *"what they are saying can u help me"*. The AI explained the deliverables, a
   suggested scope, what to leave out, and clarifying questions to ask Incubyte.
2. **Building.** One detailed prompt (below) set the goal, the fixed tech stack, the quality bar
   and the order of work. The AI then worked step by step, committing after every layer or
   feature, so the git history shows how the solution grew.
3. **Verifying.** The AI was required to prove each step: run the tests, start the app, call
   the API, drive the UI in a real browser, and look at screenshots.

## The build prompt (verbatim)

> # CONTEXT
> I am completing a hiring assessment from Incubyte for a "Software Craftsperson Java/Angular II" role.
> The task: build an Employee Salary Management web app for an HR Manager managing 10,000
> employees across multiple countries (currently they use Excel). The app must let the HR
> manager manage employee salary data and answer questions like:
> - What are min, max, average salaries per country?
> - What is the average salary for a given job title in a given country?
> - How many employees work in each country?
>
> The assessors will review my Git commit history, interview me on every design decision,
> and expect evidence of intentional AI usage. Code must be SIMPLE, CLEAN, and EASY TO EXPLAIN.
>
> # HARD CONSTRAINTS — READ CAREFULLY
> 1. DO NOT deploy anything. No hosting, no Docker-to-cloud, no live URLs. Local run only.
> 2. DO commit everything to git and push to the CURRENT branch of the repo we are working in."<local path>".
> 3. Make SMALL, INCREMENTAL commits (not one giant commit). Use clear conventional commit
>    messages (e.g., "feat: add employee entity and repository", "test: add salary stats
>    service tests"). The commit history itself is being graded.
> 4. Backend must be production-quality but NOT over-engineered. No unnecessary patterns,
>    no Spring Data REST magic I can't explain, no Lombok tricks I can't defend. Every class
>    must have a clear single purpose I can explain in one sentence.
> 5. UI must be polished and professional ("pakka"): Angular Material, clean layout, proper
>    loading states, empty states, error states, confirmation dialogs, snackbar feedback,
>    responsive table with pagination, search, and filters. It must feel like a real product.
>
> # TECH STACK (fixed)
> - Backend: Java 17, Spring Boot 3.x, Maven, H2 (file mode) or SQLite, Spring Data JPA,
>   Bean Validation, global exception handler
> - Frontend: Angular 17+, Angular Material, standalone components, HttpClient, reactive
>   forms, TypeScript strict mode
> - Tests: JUnit 5, Mockito, Spring Boot Test (slice tests), Jasmine/Karma or Jest for Angular
>
> # DATA MODEL
> Employee: id, firstName, lastName, email (unique), jobTitle, department, country,
> salary (BigDecimal), currency, hireDate, createdAt, updatedAt.
> Validation rules: all fields except none are required; salary > 0; email valid + unique;
> hireDate not in the future; currency is a valid ISO code (e.g., USD, INR, EUR, GBP).
>
> # BACKEND API
> - CRUD: GET /api/employees (paginated + searchable by name/email + filterable by country,
>   jobTitle, department; sortable by salary, hireDate, name), GET /api/employees/{id},
>   POST /api/employees, PUT /api/employees/{id}, DELETE /api/employees/{id}
> - Insights (statistics computed IN SQL, never in Java loops):
>   - GET /api/insights/salary-by-country → min, max, avg, count per country
>   - GET /api/insights/salary-by-job-title?country=X → avg salary per job title in a country
>   - GET /api/insights/headcount-by-country → employee count per country
> - Proper HTTP semantics: 201 on create, 404 on missing, 409 on duplicate email,
>   400 with field-level validation messages.
> - Add DB indexes on country, jobTitle, and email.
>
> # SCALABILITY (10,000 rows — must be real, not theoretical)
> - Server-side pagination only. NEVER load all employees into memory.
> - All statistics via aggregate SQL queries (JPQL/native query or projection interface).
> - Mention in docs why these choices were made.
>
> # WORKFLOW — FOLLOW THIS ORDER, COMMIT AFTER EACH STEP
> Step 1: Write docs/REQUIREMENTS.md — one page: goal, scope, features built, features
>         deliberately OUT of scope (login/auth, payroll/tax/payslips, currency conversion,
>         salary history/audit trail) WITH reasoning for each exclusion. Commit.
> Step 2: Scaffold Spring Boot project (entity, repository, DTO, mapper, service,
>         controller layers). Commit each layer separately.
> Step 3: Write unit tests for service layer (JUnit + Mockito) and slice tests for the
>         controller/repository. Commit.
> Step 4: Write the seed script (CommandLineRunner or Flyway data script) generating
>         10,000 realistic employees across at least 6 countries, varied job titles,
>         departments, salaries with appropriate per-country currencies. Commit.
> Step 5: Build Angular app: Employee List page (Material table, paginator, search,
>         filters, sort), Add/Edit Employee dialog or page (reactive form with validation),
>         Delete with confirmation dialog, Insights/Dashboard page (cards + tables +
>         simple Material-friendly charts for salary stats). Commit per feature.
> Step 6: Angular tests for at least one component and one service. Commit.
> Step 7: Documentation (README, REQUIREMENTS, ARCHITECTURE, DESIGN-DECISIONS, PERFORMANCE,
>         AI-PROMPTS, and a README per backend package and Angular feature folder). Commit.
>
> # QUALITY BAR
> - No dead code, no unused imports, no commented-out blocks.
> - Every public method has a short JavaDoc/JSDoc.
> - Consistent naming, formatting, and package structure.
> - Tests are deterministic (no random failures, no dependence on execution order).
> - README instructions must actually work if followed line-by-line.
>
> # BEFORE FINISHING
> 1. Run ALL backend tests and confirm they pass.
> 2. Run the backend, run the seed, run the frontend, and verify the full flow works.
> 3. Show me the full git log so I can review the commit history.
> 4. Then give me an "INTERVIEW PREP" summary.

Edits to the original: the local folder path on the development machine is replaced by
`<local path>`, and the long documentation list in Step 7 and the "before finishing" checklist
are summarised. Everything else is as typed.

## Constraints given to the AI, and why

| Constraint | Reason |
|---|---|
| Small conventional commits after every layer/feature | The history should show the thinking, not just the result. |
| No Lombok, no Spring Data REST, one purpose per class | Every line has to be explainable in an interview. |
| Statistics in SQL, server-side pagination only | Must hold up at 10,000 rows for real. |
| Prove every step (tests, running app, browser) | AI output is not trusted until it is verified. |

## What verification caught (and how it was fixed)

AI-written code was not accepted on first sight. These problems were found by running
things, not by reading code, and each fix is a separate commit:

1. **Seed visible half-way / not at all.** The first seed saved in 1,000-row chunks, and the
   API could show a partly seeded table because Spring Boot accepts requests while
   `ApplicationRunner`s are still running. Switching to one transaction then logged
   "Seeded 10000 employees" *before* the commit had flushed the inserts, so the log was
   misleading. Final version: one transaction with `saveAllAndFlush`, verified by restarting
   the app (second start logs "Skipping seed: database already has 10000 employees").
2. **Duplicate-email message too long for the form.** Found in a browser screenshot during the
   end-to-end run; the backend now sends a short field message and keeps the full sentence in
   `detail`.
3. **Edit/Delete buttons off-screen on phones.** Found in a 390 px screenshot; the list now
   shows fewer columns and tighter padding on small screens.
4. **Card header layout on the Insights page.** A wrapper `div` broke Material's header
   layout; seen in a screenshot and fixed before committing.
5. **Chrome would not start for Karma tests** on the Linux server ("No usable sandbox"). A
   `ChromeHeadlessCI` launcher with `--no-sandbox` was added for CI and servers; normal
   `npm test` is unchanged.
6. **Unrealistic demo data.** A screenshot of the list sorted by name showed the same full name
   nine times in a row: the generator had only 40 × 40 name combinations for 10,000 people.
   The pools were widened to 70 × 70; country and salary distributions stayed the same, and
   the performance numbers were re-measured on the new data.

## How the result was verified

- **Backend:** 58 JUnit tests (unit, `@DataJpaTest`, `@WebMvcTest`, one context test).
- **Frontend:** 20 Jasmine tests, run three times in random order to check they are stable.
- **API smoke test** with `curl`: 201 + `Location`, 400 field errors, 404, 409
  (case-insensitive email), 204, invalid page size and sort.
- **Browser end-to-end run** (headless Chromium, scripted): list of 10,000 → search → filter →
  sort → paginate → validation errors → add → duplicate email → edit → delete → empty state →
  phone layout. All 14 steps passed against the real backend.
- **Performance:** measured response times and H2 `EXPLAIN ANALYZE` plans
  (see [PERFORMANCE.md](PERFORMANCE.md)).
