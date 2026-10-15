# `employees` — managing employees

## `employee.model.ts`

TypeScript types that mirror the backend JSON: `Employee`, `EmployeeRequest`, `Page<T>`,
`EmployeeQuery` (everything that decides what the list shows), `FilterOptions`, and the
default query (page 0, 20 rows, sorted by name).

## `employee.service.ts` — `EmployeeService`

**What it does:** the only code that knows the `/api/employees` URLs. Methods: `list`,
`filterOptions`, `create`, `update`, `delete`, each returning an `Observable`.

**Detail:** `list` always sends paging and sort parameters and leaves out blank search and
filter values, so the backend sees exactly what the user chose.

**Why it exists:** components ask for data in domain terms (`list(query)`) instead of building
HTTP requests themselves; tests can replace it with a spy.

## `employee-list/` — `EmployeeList` (route `/employees`)

**What it does:** the main page — search box, three filter dropdowns, a sortable Material
table, the paginator, and the add / edit / delete actions.

**How it works:**

- The whole query (search, filters, sort, page, size) is **one signal**. Every user action
  creates a new query object; `toObservable(query)` + `switchMap` sends one request per change
  and **cancels** the previous one if it has not finished, so an old response can never
  overwrite a newer one.
- Search waits **300 ms** after the last keystroke (`debounceTime`) before asking the server.
- Changing a filter or the sort goes back to page 0.
- Clear **loading** (progress bar), **empty** ("No employees found" + clear filters) and
  **error** ("Could not load employees" + Try again) states.
- On screens below 900 px only name, salary and actions are shown so nothing needs
  horizontal scrolling.
- After a delete that empties the current page, it moves to the previous page.

## `employee-form-dialog/` — `EmployeeFormDialog`

**What it does:** one Material dialog for both **add** (no employee passed in) and **edit**
(an employee passed in, fields pre-filled).

**How it works:**

- A typed **reactive form** with the same rules as the backend: required fields, email
  format, salary greater than 0, hire date not in the future, currency required.
- Country, job title and department suggest existing values (autocomplete) but accept new
  ones; picking a known country pre-fills its usual currency unless the user chose one.
- On save it calls `create` or `update`; while saving the button is disabled and shows a
  spinner. On success the dialog closes with the saved employee and the list shows a
  snackbar and reloads.
- Backend errors are mapped back onto the form: `409` (email taken) and `400` field errors
  appear under the matching input; anything else appears in a banner at the top.
- The hire date is sent as `YYYY-MM-DD` using the local calendar day (see `shared/dates.ts`).

## Tests

`employee.service.spec.ts`, `employee-list/employee-list.spec.ts`,
`employee-form-dialog/employee-form-dialog.spec.ts`.
