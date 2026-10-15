// STUDY NOTES: A domain exception the service throws when an email is already used by another employee. GlobalExceptionHandler turns it into HTTP 409.
package com.acme.salary.exception;

/**
 * Thrown when a create or update would give two employees the same email. Mapped to HTTP 409.
 */
// WHY: Extends RuntimeException (unchecked), so service methods do not need "throws" clauses and Spring still rolls back the transaction.
public class DuplicateEmailException extends RuntimeException {

    /**
     * @param email the email that is already taken
     */
    public DuplicateEmailException(String email) {
        // WHY: The message names the email, so the 409 response "detail" is clear to the user.
        super("An employee with email " + email + " already exists");
    }
}
