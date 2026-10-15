# `exception` — errors and how they become HTTP responses

## `EmployeeNotFoundException`

Thrown by `EmployeeService` when an id does not exist. Becomes **404**.

## `DuplicateEmailException`

Thrown by `EmployeeService` when another employee already has the email. Becomes **409**.

## `GlobalExceptionHandler`

**What it does:** a `@RestControllerAdvice` that catches exceptions from every controller
and returns a consistent JSON error in the RFC 9457 **problem details** format:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/employees",
  "errors": { "email": "must be a well-formed email address", "salary": "must be greater than 0" }
}
```

| Situation | Status | `errors` map |
|---|---|---|
| Invalid request body (Bean Validation) | 400 | one message per field |
| Invalid query parameter (`size=500`, blank `country`) | 400 | one message per parameter |
| Malformed JSON, unknown `sortBy`, wrong type | 400 | — (handled by Spring's parent class) |
| Unknown id | 404 | — |
| Email already used | 409 | `email` → short message for the form |
| Database constraint violated (e.g. two requests creating the same email at once) | 409 | — |
| Anything unexpected | 500 | — (details are logged, not sent to the client) |

**Why it exists:** without it every controller would need try/catch blocks, and clients
would get Spring's default error pages. The `errors` map is what lets the Angular form show
each message under the right input.
