// STUDY NOTES: One central place that turns every exception from any controller into a clean JSON error (RFC 9457 ProblemDetail) with the right HTTP status.
package com.acme.salary.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into consistent JSON error responses (RFC 9457 {@link ProblemDetail}).
 * Validation errors carry an {@code errors} map of field name to message so the UI can show
 * each message next to the right input. Standard Spring MVC errors (malformed JSON, wrong
 * parameter type, ...) are handled by the parent class and also return 400.
 */
// WHY: @RestControllerAdvice applies these handlers to all controllers and writes the return value as JSON, so controllers need no try/catch.
@RestControllerAdvice
// WHY: Extending ResponseEntityExceptionHandler gives proper 400 answers for Spring's own errors (bad JSON, wrong parameter type, unknown sortBy) instead of 500.
// INTERVIEW: "ProblemDetail is the standard RFC 9457 error format built into Spring 6; the extra errors map lets the UI show each message under its field."
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // WHY: A logger to record unexpected errors on the server; static final so there is one per class.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @param ex the missing-employee error
     * @return 404 Not Found
     */
    // WHY: @ExceptionHandler says "call this method when this exception type escapes a controller".
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EmployeeNotFoundException ex) {
        // WHY: 404 with the exception's message; no field errors, so an empty map.
        return problem(HttpStatus.NOT_FOUND, "Employee not found", ex.getMessage(), Map.of());
    }

    /**
     * @param ex the duplicate-email error
     * @return 409 Conflict, with a short message attached to the {@code email} field
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException ex) {
        // WHY: 409 Conflict because the request is valid but clashes with existing data.
        return problem(HttpStatus.CONFLICT, "Duplicate email", ex.getMessage(),
                // WHY: Putting the message under the "email" key lets the Angular form show it right below the email input.
                Map.of("email", "This email is already used by another employee"));
    }

    /**
     * Safety net for a constraint violated in the database, e.g. two requests creating the
     * same email at the same moment (both pass the service check, the unique index stops one).
     *
     * @param ex the database constraint error
     * @return 409 Conflict
     */
    // WHY: Spring throws DataIntegrityViolationException when the database rejects a row (unique index, CHECK, NOT NULL).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        // WHY: A friendly fixed message; we do not send the raw SQL error to the client.
        return problem(HttpStatus.CONFLICT, "Conflicting data",
                "The change conflicts with existing data, e.g. an email that is already used", Map.of());
    }

    /**
     * Last resort for anything unexpected: logs the details and hides them from the client.
     *
     * @param ex the unexpected error
     * @return 500 Internal Server Error
     */
    // WHY: Catches every other exception, so the client always gets JSON, never a stack trace.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        // WHY: Log the full stack trace on the server so developers can debug it.
        log.error("Unexpected error", ex);
        // WHY: Generic message only: internal details could leak information to an attacker.
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
                "Something went wrong on our side. Please try again.", Map.of());
    }

    /**
     * Invalid request body (Bean Validation on {@code @Valid @RequestBody}).
     */
    // WHY: @Override of the parent method that Spring calls when @Valid fails on a request body; we replace the default body with our errors map.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        // WHY: LinkedHashMap keeps the fields in the order they were reported, so the JSON is predictable.
        Map<String, String> errors = new LinkedHashMap<>();
        // WHY: One FieldError per broken rule, e.g. email -> "must be a well-formed email address".
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // WHY: putIfAbsent keeps only the first message per field, so the UI shows one clear message under each input.
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return validationProblem(errors);
    }

    /**
     * Invalid query parameter (constraints such as {@code @Max} on controller parameters).
     */
    // WHY: Spring 6.1+ calls this when constraints on controller method parameters fail, e.g. size=500 or a blank country.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        // WHY: For each invalid parameter, use its name as the key and its first error message as the value.
        ex.getParameterValidationResults().forEach(result -> errors.putIfAbsent(
                result.getMethodParameter().getParameterName(),
                result.getResolvableErrors().get(0).getDefaultMessage()));
        return validationProblem(errors);
    }

    // WHY: Shared helper so body and parameter validation errors have exactly the same 400 shape.
    private static ResponseEntity<Object> validationProblem(Map<String, String> errors) {
        return ResponseEntity.badRequest().body(buildProblem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more fields are invalid", errors));
    }

    // WHY: Small helper that wraps a ProblemDetail in a ResponseEntity with the given status, used by the @ExceptionHandler methods.
    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail,
                                                         Map<String, String> errors) {
        return ResponseEntity.status(status).body(buildProblem(status, title, detail, errors));
    }

    // WHY: The one place that builds the error body, so every error response looks the same.
    private static ProblemDetail buildProblem(HttpStatus status, String title, String detail,
                                              Map<String, String> errors) {
        // WHY: forStatusAndDetail fills "status" and "detail"; Spring adds "type" and "instance".
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        // WHY: Only add the "errors" property when there are field messages, so other errors stay clean.
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}
