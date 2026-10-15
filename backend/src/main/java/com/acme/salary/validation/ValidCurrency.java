// STUDY NOTES: Our own Bean Validation annotation, @ValidCurrency. It works like @NotBlank or @Email, and its message lands in the same "errors" map of a 400 response.
package com.acme.salary.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field that must hold an ISO 4217 currency code such as "USD" or "INR".
 * A {@code null} value is accepted; combine with {@code @NotBlank} to make it required.
 */
// WHY: @Documented makes the annotation show up in the Javadoc of fields that use it.
@Documented
// WHY: @Constraint tells Bean Validation which class does the actual check.
@Constraint(validatedBy = CurrencyCodeValidator.class)
// WHY: Allowed on fields; on a record component (like EmployeeRequest.currency) the annotation is passed to the generated field.
@Target(ElementType.FIELD)
// WHY: RUNTIME so the validator can still see the annotation while the app runs.
@Retention(RetentionPolicy.RUNTIME)
// WHY: "@interface" declares an annotation type.
public @interface ValidCurrency {

    // WHY: message, groups and payload are the three members every Bean Validation constraint must have.
    /** @return message shown when the code is not a known currency */
    String message() default "must be a valid ISO 4217 currency code, e.g. USD";

    /** @return validation groups (unused, required by the spec) */
    Class<?>[] groups() default {};

    /** @return payload (unused, required by the spec) */
    Class<? extends Payload>[] payload() default {};
}
