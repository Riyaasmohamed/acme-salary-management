# `controller` — the HTTP API

Controllers translate HTTP requests into service calls and service results into HTTP
responses. They contain no business rules.

## `EmployeeController` — `/api/employees`

| Method & path | Success | Errors |
|---|---|---|
| `GET /api/employees` | 200, one page | 400 for bad `page`, `size` (> 100) or `sortBy` |
| `GET /api/employees/filter-options` | 200 | — |
| `GET /api/employees/{id}` | 200 | 404 |
| `POST /api/employees` | **201** + `Location` header | 400 (validation), 409 (email taken) |
| `PUT /api/employees/{id}` | 200 | 400, 404, 409 |
| `DELETE /api/employees/{id}` | **204** | 404 |

List parameters: `search`, `country`, `jobTitle`, `department` (all optional), `page`
(default 0), `size` (default 20, 1–100), `sortBy` (`NAME` | `SALARY` | `HIRE_DATE`),
`direction` (`ASC` | `DESC`).

**Responsibility:** read parameters, trigger validation (`@Valid` on the body, `@Min`/`@Max`
on query parameters), build the `PageRequest`, choose the status code.

## `InsightsController` — `/api/insights`

| Method & path | Answers |
|---|---|
| `GET /api/insights/salary-by-country` | min / max / average salary and headcount per country |
| `GET /api/insights/salary-by-job-title?country=India` | average (and min / max) salary per job title in that country; 400 if `country` is missing or blank |
| `GET /api/insights/headcount-by-country` | number of employees per country |

**Why these classes exist:** they are the only place that knows about URLs and HTTP status
codes, so the services stay reusable and easy to test. Their HTTP behaviour is tested with
`@WebMvcTest` (`EmployeeControllerTest`, `InsightsControllerTest`).
