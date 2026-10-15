// STUDY NOTES: A domain exception the service throws when an id does not exist. GlobalExceptionHandler turns it into HTTP 404.
package com.acme.salary.exception;

/**
 * Thrown when an employee id does not exist. Mapped to HTTP 404.
 */
// WHY: Unchecked (RuntimeException) so it can travel up from the service to the handler without try/catch in the controller.
public class EmployeeNotFoundException extends RuntimeException {

    /**
     * @param id the id that was looked up
     */
    public EmployeeNotFoundException(Long id) {
        // WHY: Includes the id so the error message says exactly which employee was missing.
        super("Employee " + id + " was not found");
    }
}
