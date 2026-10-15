// STUDY NOTES: Input DTO: the JSON body for creating or updating an employee. Its annotations are the validation rules that @Valid in the controller checks.
package com.acme.salary.dto;

import com.acme.salary.validation.ValidCurrency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of the create and update requests. Bean Validation rules here are the single
 * source of truth for what a valid employee looks like.
 *
 * @param firstName  first name
 * @param lastName   last name
 * @param email      work email, unique across employees (case-insensitive)
 * @param jobTitle   job title
 * @param department department
 * @param country    country of employment
 * @param salary     annual salary, greater than zero, at most two decimals
 * @param currency   ISO 4217 currency code of the salary
 * @param hireDate   date the employee joined, not in the future
 */
// WHY: There is no id, createdAt or updatedAt here, so a client can never set them. The server owns those values.
// INTERVIEW: "Validation lives on the request DTO, not on the entity: it describes what a request may contain, and the entity stays a pure table mapping."
public record EmployeeRequest(
        // WHY: @NotBlank rejects null, "" and "   ". @Size(max = 100) matches VARCHAR(100) in the table, so a long value gets a clear 400, not a database error.
        @NotBlank @Size(max = 100) String firstName,
        // WHY: Same rules as firstName, for the same reasons.
        @NotBlank @Size(max = 100) String lastName,
        // WHY: @Email checks the format; 254 is the longest valid email and matches the column. Uniqueness is checked later in the service.
        @NotBlank @Email @Size(max = 254) String email,
        // WHY: Required free text, limited to the column length.
        @NotBlank @Size(max = 100) String jobTitle,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 100) String country,
        // WHY: @NotNull because a number cannot be "blank"; @Positive rejects 0 and negatives; @Digits(12, 2) matches DECIMAL(14,2) so no cents are lost. BigDecimal keeps money exact.
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal salary,
        // WHY: @NotBlank makes it required; our custom @ValidCurrency checks it is a real ISO 4217 code (each annotation checks one thing).
        @NotBlank @ValidCurrency String currency,
        // WHY: LocalDate is a calendar day with no time zone; @PastOrPresent stops hire dates in the future.
        @NotNull @PastOrPresent LocalDate hireDate) {
}
