// STUDY NOTES: Not a test itself. It is a "test data builder" helper (Object Mother pattern) used by many
// STUDY NOTES: test classes. Keeping fixed, readable sample data in one place removes copy-paste and keeps
// STUDY NOTES: tests short, so each test only shows the values that matter for it.
package com.acme.salary.support;

import com.acme.salary.dto.EmployeeRequest;
import com.acme.salary.entity.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fixed, readable test data shared by the test classes.
 */
// WHY: final + private constructor = utility class. Nobody should create or extend it; only call its static methods.
public final class EmployeeTestData {

    private EmployeeTestData() {
    }

    /**
     * @return a valid create/update request for an engineer in India
     */
    // WHY: One known-good request. Tests start from valid data and change only one field
    // WHY: when they want to check one rule. That makes the reason for a failure obvious.
    public static EmployeeRequest validRequest() {
        // WHY: Fixed values (no random, no LocalDate.now()) so every run is the same. BigDecimal from a
        // WHY: String keeps exact money values (no floating-point rounding).
        return new EmployeeRequest("Asha", "Rao", "asha.rao@acme.com", "Software Engineer", "Engineering",
                "India", new BigDecimal("1500000.00"), "INR", LocalDate.of(2022, 4, 1));
    }

    /**
     * @param email email to use
     * @return {@link #validRequest()} with a different email
     */
    // WHY: Email must be unique, so tests that need a second (or duplicate) employee need a way to set it.
    public static EmployeeRequest validRequestWithEmail(String email) {
        EmployeeRequest r = validRequest();
        // WHY: EmployeeRequest is an immutable record, so we build a new one copying every field except email.
        return new EmployeeRequest(r.firstName(), r.lastName(), email, r.jobTitle(), r.department(),
                r.country(), r.salary(), r.currency(), r.hireDate());
    }

    /**
     * @return an unsaved entity with the given values and fixed defaults for the rest
     */
    // WHY: Builds an entity (not a request) for repository and service tests. Only the fields that
    // WHY: matter for grouping and statistics (job, department, country, salary, currency) are parameters.
    public static Employee employee(String firstName, String lastName, String jobTitle, String department,
                                    String country, String salary, String currency) {
        // WHY: No id is set. The entity is "unsaved"; the database gives the id when it is persisted.
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        // WHY: Email is derived from the name so it is unique per person, which the unique index requires.
        employee.setEmail((firstName + "." + lastName + "@acme.com").toLowerCase());
        employee.setJobTitle(jobTitle);
        employee.setDepartment(department);
        employee.setCountry(country);
        // WHY: Salary is passed as a String so tests can write exact decimals like "1000.00".
        employee.setSalary(new BigDecimal(salary));
        employee.setCurrency(currency);
        // WHY: A fixed hire date keeps the data deterministic (no dependency on today's date).
        employee.setHireDate(LocalDate.of(2020, 1, 15));
        return employee;
    }
}
