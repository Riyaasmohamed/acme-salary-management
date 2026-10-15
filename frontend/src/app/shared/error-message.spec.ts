// STUDY NOTES: Checks the helpers that turn any HTTP error into a message for the user (errorMessage) and
// STUDY NOTES: into per-field form errors (fieldErrors). Plain UNIT test of pure functions: we build fake
// STUDY NOTES: HttpErrorResponse objects by hand, so no HTTP call and no TestBed are needed.
// WHY: The real Angular error class. The helpers use "instanceof HttpErrorResponse", so we must use it too.
import { HttpErrorResponse } from '@angular/common/http';

import { errorMessage, fieldErrors } from './error-message';

describe('error helpers', () => {
  // WHY: status 0 is what Angular reports when the request never reached the server (backend down,
  // WHY: no network). The user needs a hint, not "HTTP 0".
  it('explains when the server cannot be reached', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Cannot reach the server');
  });

  // WHY: Proves the backend's ProblemDetail "detail" text is shown as-is. The backend owns the wording.
  it('uses the problem detail sent by the backend', () => {
    // WHY: Arrange: fake a 404 with the same body shape GlobalExceptionHandler returns.
    const error = new HttpErrorResponse({ status: 404, error: { detail: 'Employee 9 was not found' } });

    // WHY: toBe checks the exact string.
    expect(errorMessage(error)).toBe('Employee 9 was not found');
  });

  // WHY: Some errors have no JSON body (for example a 502 from a proxy). We still show something useful.
  it('falls back to the status code when there is no detail', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 502, error: null }))).toBe('The request failed (HTTP 502).');
  });

  // WHY: The form dialog uses fieldErrors to show each server message under the right input.
  it('returns field errors from a validation problem, or an empty map', () => {
    // WHY: Arrange: a 400 body with an "errors" map, like the backend's validation response.
    const error = new HttpErrorResponse({
      status: 400,
      error: { errors: { email: 'must be a well-formed email address' } }
    });

    // WHY: toEqual compares object contents (not the same object reference).
    expect(fieldErrors(error)).toEqual({ email: 'must be a well-formed email address' });
    // WHY: A non-HTTP error gives an empty map, so the calling code never has to check for undefined.
    expect(fieldErrors(new Error('boom'))).toEqual({});
  });
});
