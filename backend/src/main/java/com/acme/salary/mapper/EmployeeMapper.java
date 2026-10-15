// STUDY NOTES: Copies data between the API DTOs and the Employee entity, and cleans up the input (trim, lower-case email, upper-case currency) in one place.
package com.acme.salary.mapper;

import com.acme.salary.dto.EmployeeRequest;
import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.entity.Employee;

import java.util.Locale;

/**
 * Converts between the API DTOs and the {@link Employee} entity. Also normalises input in
 * one place: text is trimmed, email is lower-cased and currency is upper-cased, so
 * "John@Acme.com " and "john@acme.com" are treated as the same email.
 */
// WHY: final class with only static methods: it has no state, so there is nothing to create or inject.
// INTERVIEW: "Hand-written instead of MapStruct: one entity with nine fields; a short class anyone can read beats a code generator."
public final class EmployeeMapper {

    // WHY: Private constructor so nobody can create an instance of a utility class.
    private EmployeeMapper() {
    }

    /**
     * @param request validated request body
     * @return a new, unsaved entity
     */
    public static Employee toEntity(EmployeeRequest request) {
        // WHY: New entity has no id yet, so save() will INSERT it.
        Employee employee = new Employee();
        // WHY: Reuse copyInto so create and update apply exactly the same normalisation.
        copyInto(request, employee);
        return employee;
    }

    /**
     * Overwrites every editable field of an existing entity.
     *
     * @param request  validated request body
     * @param employee entity loaded from the database
     */
    public static void copyInto(EmployeeRequest request, Employee employee) {
        // WHY: trim() removes spaces the user typed by mistake at the start or end.
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        // WHY: Email goes through normaliseEmail so the unique check works regardless of case.
        employee.setEmail(normaliseEmail(request.email()));
        employee.setJobTitle(request.jobTitle().trim());
        employee.setDepartment(request.department().trim());
        employee.setCountry(request.country().trim());
        // WHY: Salary is copied as is; BigDecimal is already exact and validation checked it.
        employee.setSalary(request.salary());
        // WHY: Upper-case so "inr" is stored as "INR". Locale.ROOT avoids language-specific rules (e.g. Turkish "i").
        employee.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        employee.setHireDate(request.hireDate());
    }

    /**
     * @param employee entity
     * @return the API representation
     */
    public static EmployeeResponse toResponse(Employee employee) {
        // WHY: Builds the response record field by field, including id and timestamps, which only the server sets.
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getJobTitle(),
                employee.getDepartment(),
                employee.getCountry(),
                employee.getSalary(),
                employee.getCurrency(),
                employee.getHireDate(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }

    /**
     * @param email raw email from the request
     * @return the email in the form it is stored and compared in
     */
    // WHY: public because the service also uses it before calling existsByEmail, so the check compares the same form that is stored.
    public static String normaliseEmail(String email) {
        // WHY: "John@ACME.com " becomes "john@acme.com"; Locale.ROOT for language-neutral lower-casing.
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
