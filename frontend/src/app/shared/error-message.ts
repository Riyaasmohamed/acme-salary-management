// STUDY NOTES: Helpers that turn an HTTP error into text for the user. errorMessage() gives one sentence; fieldErrors() gives the per-field messages for forms.
// STUDY NOTES: The backend returns errors as RFC 9457 "problem details" JSON (Spring's ProblemDetail), with an extra "errors" map for field validation.
// WHY: HttpErrorResponse is the error type HttpClient produces when a request fails; we check for it with instanceof.
import { HttpErrorResponse } from '@angular/common/http';

/**
 * Error body returned by the backend (RFC 9457 problem details).
 */
// WHY: An interface describes the JSON shape for TypeScript only (like a Java DTO without code); the '?' marks each field as optional.
export interface ProblemDetail {
  // WHY: Short title of the error, e.g. "Bad Request".
  title?: string;
  // WHY: Human-readable explanation; this is the text we show to the user.
  detail?: string;
  // WHY: The HTTP status code repeated in the body.
  status?: number;
  // WHY: Record<string, string> is a map of field name → message, e.g. { email: "This email is already used…" }.
  errors?: Record<string, string>;
}

/**
 * Turns any error into one sentence that can be shown to the HR Manager.
 */
// WHY: The parameter is "unknown" because RxJS catchError can receive any kind of error; we must check the type before using it.
export function errorMessage(error: unknown): string {
  // WHY: instanceof narrows the type, so inside this block TypeScript knows error has status and error.error.
  if (error instanceof HttpErrorResponse) {
    // WHY: Status 0 means no HTTP response at all (backend down, network problem), so we give a helpful hint.
    if (error.status === 0) {
      return 'Cannot reach the server. Please check that the backend is running.';
    }
    // WHY: error.error is the parsed response body; we tell TypeScript to treat it as a ProblemDetail (or null).
    const problem = error.error as ProblemDetail | null;
    // WHY: ?. stops safely if problem is null; ?? uses the fallback text when there is no "detail".
    return problem?.detail ?? `The request failed (HTTP ${error.status}).`;
  }
  // WHY: Any non-HTTP error (a bug in code) still gets a friendly message instead of a technical one.
  return 'Something went wrong. Please try again.';
}

/**
 * Returns the backend's field-to-message map (validation or duplicate email), or an empty map.
 */
export function fieldErrors(error: unknown): Record<string, string> {
  // WHY: Only HTTP errors can carry a field map from the backend.
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | null;
    // WHY: Returns an empty map instead of undefined, so callers can loop over it without null checks.
    return problem?.errors ?? {};
  }
  return {};
}
