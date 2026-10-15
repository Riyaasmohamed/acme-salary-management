// STUDY NOTES: Checks EmployeeDataGenerator, which creates the 10,000 demo employees. Plain UNIT test:
// STUDY NOTES: the generator is a pure Java class (seed in, list out), so we call it directly with no Spring
// STUDY NOTES: and no mocks. We check the PROPERTIES of the output (count, unique emails, repeatable, valid)
// STUDY NOTES: instead of exact names, because those are random.
package com.acme.salary.seed;

import com.acme.salary.entity.Employee;
// WHY: JUnit 5 test annotation.
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// WHY: AssertJ fluent assertions (hasSize, allSatisfy, extracting ...).
import static org.assertj.core.api.Assertions.assertThat;
// WHY: tuple(...) groups several values so we can compare many fields of each employee at once.
import static org.assertj.core.groups.Tuple.tuple;

class EmployeeDataGeneratorTest {

    // WHY: A fixed "today" instead of LocalDate.now(). The generator takes the date as a parameter,
    // WHY: so the test is deterministic: it gives the same result on any day it runs.
    // INTERVIEW: Passing the clock/date in (instead of calling now() inside) is what makes time-based code testable.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    // WHY: Arrange once for all tests: same seed 42 and same count as the real app config.
    // WHY: JUnit creates a new test-class instance per test, so each test gets this fresh list.
    private final List<Employee> employees = new EmployeeDataGenerator(42).generate(10_000, TODAY);

    // WHY: Proves the generator honours the "count" argument (app.seed.employee-count).
    @Test
    void createsTheRequestedNumberOfEmployees() {
        assertThat(employees).hasSize(10_000);
    }

    // WHY: The database has a unique index on email. If two generated emails clash, the seed
    // WHY: would crash at startup. This test proves the "asha.rao2@acme.com" numbering works.
    @Test
    void everyEmailIsUnique() {
        // WHY: Act: put all emails into a Set. A Set drops duplicates.
        Set<String> emails = employees.stream().map(Employee::getEmail).collect(Collectors.toSet());

        // WHY: Assert: if the Set still has 10,000 items, no email was repeated.
        assertThat(emails).hasSize(10_000);
    }

    // WHY: Proves the same seed always gives the same data. Demos and tests stay repeatable.
    @Test
    void sameSeedProducesTheSameEmployees() {
        // WHY: Act: generate a second time with the same seed and date.
        List<Employee> again = new EmployeeDataGenerator(42).generate(10_000, TODAY);

        // WHY: extracting(...) pulls email, salary and country from each employee as a tuple.
        // WHY: containsExactlyElementsOf checks same values AND same order, compared to the first list.
        // WHY: We compare fields, not objects, because Employee does not need a value-based equals().
        assertThat(again).extracting(Employee::getEmail, Employee::getSalary, Employee::getCountry)
                .containsExactlyElementsOf(employees.stream()
                        .map(e -> tuple(e.getEmail(), e.getSalary(), e.getCountry()))
                        .toList());
    }

    // WHY: The insights screens need several countries, and each country must use one currency
    // WHY: (salary stats per country would be meaningless if INR and USD were mixed).
    @Test
    void coversAtLeastSixCountriesEachWithOneCurrency() {
        // WHY: Build a map: country -> set of currencies used by employees in that country.
        Map<String, Set<String>> currenciesByCountry = employees.stream().collect(Collectors.groupingBy(
                Employee::getCountry, Collectors.mapping(Employee::getCurrency, Collectors.toSet())));

        // WHY: Enough variety for a useful demo.
        assertThat(currenciesByCountry).hasSizeGreaterThanOrEqualTo(6);
        // WHY: allSatisfy runs the inner assertion on every value: each country has exactly one currency.
        assertThat(currenciesByCountry.values()).allSatisfy(currencies -> assertThat(currencies).hasSize(1));
        // WHY: One concrete example to prove the mapping is the RIGHT currency, not just a single one.
        assertThat(currenciesByCountry.get("India")).containsExactly("INR");
    }

    // WHY: Seeded rows must look like rows a user could create through the API. Otherwise editing a
    // WHY: seeded employee in the UI could fail validation, which would confuse the HR manager.
    @Test
    void everyEmployeePassesTheApiValidationRules() {
        // WHY: allSatisfy checks every one of the 10,000 employees and reports the ones that fail.
        assertThat(employees).allSatisfy(employee -> {
            // WHY: Salary must be positive (matches the @Positive rule on the request).
            assertThat(employee.getSalary()).isGreaterThan(BigDecimal.ZERO);
            // WHY: No hire dates in the future.
            assertThat(employee.getHireDate()).isBeforeOrEqualTo(TODAY);
            // WHY: Lower-case letters and dots, an optional number for duplicates, then @acme.com.
            // WHY: This also proves the apostrophe in names like O'Brien is removed.
            assertThat(employee.getEmail()).matches("[a-z.]+\\d*@acme\\.com");
            assertThat(employee.getJobTitle()).isNotBlank();
            assertThat(employee.getDepartment()).isNotBlank();
        });
    }
}
