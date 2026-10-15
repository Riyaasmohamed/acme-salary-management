// STUDY NOTES: Output DTO: what the API sends back for one employee. It is separate from the Employee entity so the JSON contract stays stable when the table changes.
package com.acme.salary.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An employee as returned by the API. Separate from the entity so the JSON shape does not
 * change when the database mapping does.
 *
 * @param id         database id
 * @param firstName  first name
 * @param lastName   last name
 * @param email      work email
 * @param jobTitle   job title
 * @param department department
 * @param country    country of employment
 * @param salary     annual salary
 * @param currency   ISO 4217 currency code of the salary
 * @param hireDate   date the employee joined
 * @param createdAt  when the record was created
 * @param updatedAt  when the record was last changed
 */
// INTERVIEW: "We never return the JPA entity: the DTO avoids lazy-loading surprises and lets the table change without breaking the UI."
public record EmployeeResponse(
        // WHY: Long (object) because the id comes from the database; the UI needs it for edit and delete URLs.
        Long id,
        // WHY: The editable fields, the same as in EmployeeRequest.
        String firstName,
        String lastName,
        String email,
        String jobTitle,
        String department,
        String country,
        // WHY: BigDecimal so the exact amount is sent as a JSON number, no rounding errors.
        BigDecimal salary,
        String currency,
        // WHY: LocalDate is written as "YYYY-MM-DD", a plain calendar day.
        LocalDate hireDate,
        // WHY: Instant is an exact moment in UTC; these audit timestamps are read-only for the client.
        Instant createdAt,
        Instant updatedAt) {
}
