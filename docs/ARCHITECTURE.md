# Architecture

## Overview

A classic three-tier web application: an Angular single-page app talks JSON over HTTP to a
Spring Boot REST API, which stores data in an H2 relational database (file mode).

```mermaid
flowchart LR
    HR([HR Manager<br/>browser])

    subgraph FE["Frontend · Angular 20 + Material (port 4200)"]
        direction TB
        Pages["Pages<br/>EmployeeList · InsightsPage"]
        Dialogs["Dialogs<br/>EmployeeFormDialog · ConfirmDialog"]
        Services["API services<br/>EmployeeService · InsightsService"]
        Pages --> Services
        Dialogs --> Services
    end

    subgraph BE["Backend · Spring Boot 3.5 (port 8080)"]
        direction TB
        Controllers["Controllers<br/>EmployeeController · InsightsController"]
        Advice["GlobalExceptionHandler<br/>(RFC 9457 problem details)"]
        BServices["Services<br/>EmployeeService · InsightsService"]
        Repo["EmployeeRepository<br/>Spring Data JPA + Specifications"]
        Seeder["EmployeeSeeder<br/>(startup, empty DB only)"]
        Controllers --> BServices --> Repo
        Controllers -.errors.-> Advice
        Seeder --> Repo
    end

    DB[("H2 database (file)<br/>table employee<br/>schema by Flyway")]

    HR --> Pages
    Services -- "/api/** via dev-server proxy" --> Controllers
    Repo -- "SQL (JPQL, GROUP BY)" --> DB
```

## Backend layers

Each layer has one job and only talks to the layer below it.

| Layer (package) | Responsibility | Knows about |
|---|---|---|
| `controller` | HTTP: URLs, query parameters, status codes (201, 204), request validation trigger (`@Valid`) | services, DTOs |
| `exception` | Turns exceptions into JSON error responses (404, 409, 400 with field messages) | domain exceptions |
| `service` | Business rules: unique email, not-found handling, rounding averages, transactions | repository, mapper, DTOs |
| `mapper` | Converts DTO ⇄ entity and normalises input (trim, lower-case email, upper-case currency) | DTOs, entity |
| `repository` | Database access: CRUD, paged search, aggregate SQL queries | entity |
| `entity` | The `Employee` table mapping | — |
| `dto` | The JSON contract of the API (Java records) and request validation rules | — |
| `validation` | Custom `@ValidCurrency` constraint (ISO 4217) | — |
| `seed` + `config` | Demo data generation and its settings | repository, entity |

Every package has its own `README.md` describing each class.

## Request flow: listing employees

```mermaid
sequenceDiagram
    autonumber
    actor HR as HR Manager
    participant UI as EmployeeList (Angular)
    participant API as EmployeeService (Angular)
    participant C as EmployeeController
    participant S as EmployeeService (Java)
    participant R as EmployeeRepository
    participant DB as H2

    HR->>UI: types "patel", picks country "India"
    Note over UI: search is debounced 300 ms<br/>query signal changes → switchMap
    UI->>API: list(query)
    API->>C: GET /api/employees?search=patel&country=India&page=0&size=20&sortBy=NAME&direction=ASC
    C->>C: validate page ≥ 0, 1 ≤ size ≤ 100,<br/>sortBy ∈ {NAME, SALARY, HIRE_DATE}
    C->>S: list(search, country, …, PageRequest)
    S->>R: findAll(Specification, Pageable)
    R->>DB: SELECT … WHERE … ORDER BY … LIMIT 20 OFFSET 0
    R->>DB: SELECT COUNT(*) WHERE …
    DB-->>R: 20 rows + total
    R-->>S: Page<Employee>
    S-->>C: PageResponse<EmployeeResponse>
    C-->>API: 200 JSON {content, page, size, totalElements, totalPages}
    API-->>UI: Page
    UI-->>HR: table + paginator ("50 matching")
```

## Request flow: creating an employee

```mermaid
sequenceDiagram
    autonumber
    actor HR as HR Manager
    participant D as EmployeeFormDialog
    participant C as EmployeeController
    participant S as EmployeeService (Java)
    participant R as EmployeeRepository
    participant DB as H2

    HR->>D: fills the form, clicks "Add employee"
    D->>D: client validation (same rules as backend)
    D->>C: POST /api/employees {…}
    C->>C: Bean Validation on EmployeeRequest
    alt invalid
        C-->>D: 400 {errors: {field: message}}
        D-->>HR: message under each field
    else valid
        C->>S: create(request)
        S->>R: existsByEmail(normalised email)
        alt email taken
            S-->>C: DuplicateEmailException
            C-->>D: 409 {errors: {email: "This email is already used…"}}
        else free
            S->>R: save(entity)
            R->>DB: INSERT (unique index on email is the final guard)
            C-->>D: 201 Created + Location: /api/employees/{id}
            D-->>HR: dialog closes, snackbar "… was added", list reloads
        end
    end
```

## Request flow: insights

`GET /api/insights/salary-by-country` → `InsightsController` → `InsightsService` →
`EmployeeRepository.salaryStatsByCountry()`, which runs a single
`SELECT country, currency, MIN, MAX, AVG, COUNT … GROUP BY country, currency`.
The database returns 8 small rows; Java only rounds the averages and renames fields.
No employee entity is loaded for any insight.

## Frontend structure

```
src/app
├── app.ts / app.html        shell: toolbar navigation + <router-outlet>
├── app.routes.ts            /employees, /insights (lazy-loaded pages)
├── employees/               list page, add/edit dialog, model, API service
├── insights/                dashboard page, model, API service
└── shared/                  confirm dialog, bar chart, notifications, error/date helpers
```

- **State**: each page keeps its state in Angular **signals** (`query`, `page`, `loading`,
  `error`). Templates read signals, so the view updates automatically.
- **Async**: HTTP calls are RxJS observables. The list turns its `query` signal into an
  observable and uses `switchMap`, so a newer query cancels an older in-flight request.
- **Routing**: both pages are lazy-loaded (`loadComponent`), so each page's code is
  downloaded only when it is opened.
- **API access**: only the two `*.service.ts` files know URLs. Components never call
  `HttpClient` directly.
- **Dev proxy**: `proxy.conf.json` forwards `/api` from `localhost:4200` to
  `localhost:8080`, so the browser sees one origin and no CORS configuration is needed.

## Data model

```mermaid
erDiagram
    EMPLOYEE {
        BIGINT id PK "from sequence employee_seq (allocation 50)"
        VARCHAR first_name "NOT NULL, ≤100"
        VARCHAR last_name "NOT NULL, ≤100"
        VARCHAR email UK "NOT NULL, lower-case, unique"
        VARCHAR job_title "NOT NULL, indexed"
        VARCHAR department "NOT NULL, indexed"
        VARCHAR country "NOT NULL, indexed"
        DECIMAL salary "DECIMAL(14,2), CHECK > 0"
        VARCHAR currency "ISO 4217, 3 letters"
        DATE hire_date "not in the future"
        TIMESTAMP created_at "set on insert"
        TIMESTAMP updated_at "set on every update"
    }
```

One table is enough for the scope (see [REQUIREMENTS.md](REQUIREMENTS.md)). Salary history
would add a second, append-only `salary_change` table.
