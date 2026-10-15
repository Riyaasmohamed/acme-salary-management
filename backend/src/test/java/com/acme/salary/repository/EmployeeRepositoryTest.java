// STUDY NOTES: Checks EmployeeRepository and EmployeeSpecifications against a REAL database (in-memory H2
// STUDY NOTES: with the real Flyway migration). It is a SLICE test (@DataJpaTest): only the JPA layer starts,
// STUDY NOTES: no controllers or services. We chose it because the logic here IS the SQL (GROUP BY, ORDER BY,
// STUDY NOTES: LIKE escaping, unique index). Mocking the repository would test nothing real.
package com.acme.salary.repository;

import com.acme.salary.dto.EmployeeSortField;
import com.acme.salary.entity.Employee;
import com.acme.salary.support.EmployeeTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
// WHY: assertThatThrownBy checks that a piece of code throws the expected exception.
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// WHY: within(...) gives an allowed error margin when comparing decimal numbers.
import static org.assertj.core.api.Assertions.within;

/**
 * Runs the real Flyway schema and the real queries against an in-memory H2 database.
 * Every test starts from the same five employees and is rolled back afterwards.
 */
// WHY: @DataJpaTest starts only JPA: entities, repositories, the DataSource and Flyway. It is much
// WHY: faster than @SpringBootTest. Each test runs in a transaction that is ROLLED BACK at the end,
// WHY: so tests never see each other's data and can run in any order.
// INTERVIEW: "Why not mock the repository?" Because a mock only returns what I tell it. Only a real
// INTERVIEW: database can prove my JPQL GROUP BY, sorting and unique constraint actually work.
@DataJpaTest
class EmployeeRepositoryTest {

    // WHY: Spring gives us the real repository bean (a Spring Data proxy), not a mock.
    @Autowired
    private EmployeeRepository repository;

    // WHY: Kept in a field because one test needs Asha's generated id.
    private Employee asha;

    // WHY: @BeforeEach runs before EVERY test. Each test starts from the same 5 known employees:
    // WHY: 3 in India (INR) and 2 in the United States (USD). Small data lets us calculate the
    // WHY: expected numbers by hand.
    @BeforeEach
    void saveEmployees() {
        asha = EmployeeTestData.employee("Asha", "Rao", "Software Engineer", "Engineering", "India", "1000000", "INR");
        // WHY: saveAllAndFlush writes the rows to the database now (flush), so the queries below see them.
        repository.saveAllAndFlush(List.of(
                asha,
                EmployeeTestData.employee("Ravi", "Kumar", "Software Engineer", "Engineering", "India", "1400000", "INR"),
                EmployeeTestData.employee("Meera", "Iyer", "Product Manager", "Product", "India", "2000000", "INR"),
                EmployeeTestData.employee("John", "Smith", "Software Engineer", "Engineering", "United States", "120000", "USD"),
                EmployeeTestData.employee("Emma", "Brown", "Data Analyst", "Data", "United States", "90000", "USD")));
    }

    // WHY: Proves the min/max/avg/count are calculated by the database with GROUP BY, per country.
    @Test
    void salaryStatsByCountryAggregatesEachCountryInTheDatabase() {
        // WHY: Act: run the real JPQL aggregate query.
        List<SalaryStatsRow> rows = repository.salaryStatsByCountry();

        // WHY: Assert: one row per country, ordered by country name (India before United States).
        assertThat(rows).hasSize(2);
        SalaryStatsRow india = rows.get(0);
        assertThat(india.groupKey()).isEqualTo("India");
        assertThat(india.currency()).isEqualTo("INR");
        // WHY: isEqualByComparingTo ignores BigDecimal scale, so 1000000 equals 1000000.00.
        // WHY: Plain isEqualTo would fail because BigDecimal.equals also compares the scale.
        assertThat(india.minSalary()).isEqualByComparingTo("1000000");
        assertThat(india.maxSalary()).isEqualByComparingTo("2000000");
        // WHY: (1,000,000 + 1,400,000 + 2,000,000) / 3 = 1,466,666.67. The average is a double,
        // WHY: so we allow a tiny error with within(0.01) instead of an exact match.
        assertThat(india.averageSalary()).isCloseTo(1466666.67, within(0.01));
        assertThat(india.employeeCount()).isEqualTo(3);
        SalaryStatsRow unitedStates = rows.get(1);
        assertThat(unitedStates.groupKey()).isEqualTo("United States");
        // WHY: (120,000 + 90,000) / 2 = 105,000 exactly, so an exact compare is safe here.
        assertThat(unitedStates.averageSalary()).isEqualTo(105000.0);
        assertThat(unitedStates.employeeCount()).isEqualTo(2);
    }

    // WHY: Proves the WHERE country filter works (US rows are not counted) and the order is
    // WHY: highest average salary first.
    @Test
    void salaryStatsByJobTitleOnlyCountsTheGivenCountryHighestAverageFirst() {
        List<SalaryStatsRow> rows = repository.salaryStatsByJobTitle("India");

        // WHY: extracting pulls one field from each row; containsExactly also checks the order.
        // WHY: Product Manager (2,000,000) comes before Software Engineer (avg 1,200,000).
        assertThat(rows).extracting(SalaryStatsRow::groupKey).containsExactly("Product Manager", "Software Engineer");
        // WHY: Only Asha and Ravi count; John (a US Software Engineer) must be excluded.
        assertThat(rows.get(1).averageSalary()).isEqualTo(1200000.0);
        assertThat(rows.get(1).employeeCount()).isEqualTo(2);
    }

    // WHY: Proves headcount is grouped per country and sorted largest first.
    @Test
    void headcountByCountryIsOrderedLargestFirst() {
        // WHY: HeadcountRow is a record, so equals() compares values and we can compare whole rows.
        assertThat(repository.headcountByCountry()).containsExactly(
                new HeadcountRow("India", 3L), new HeadcountRow("United States", 2L));
    }

    // WHY: Proves the search box behaviour: case-insensitive, matches first name, full name and email.
    @Test
    void searchMatchesNameAndEmailIgnoringCase() {
        // WHY: Upper-case input still finds "Ravi" (both sides are lower-cased in SQL).
        assertThat(search("RAVI")).containsExactly("Ravi");
        // WHY: "asha rao" matches the concatenated full name "first last".
        assertThat(search("asha rao")).containsExactly("Asha");
        // WHY: Part of an email also matches.
        assertThat(search("john.smith@")).containsExactly("John");
    }

    // WHY: In SQL LIKE, "_" means "any one character" and "%" means "anything". If we did not
    // WHY: escape them, searching "_" would match every employee. Empty results prove they are
    // WHY: treated as plain text.
    @Test
    void searchTreatsLikeWildcardsAsPlainText() {
        assertThat(search("_")).isEmpty();
        assertThat(search("%")).isEmpty();
    }

    // WHY: Proves several filters are combined with AND, not OR: India AND Product = only Meera.
    // WHY: null values mean "no filter" for that field.
    @Test
    void filtersAreCombinedWithAnd() {
        List<Employee> result = repository.findAll(
                EmployeeSpecifications.matching(null, "India", null, "Product"));

        assertThat(result).extracting(Employee::getFirstName).containsExactly("Meera");
    }

    // WHY: Proves paging and sorting are done in the database and the total count is correct,
    // WHY: which the UI paginator needs.
    @Test
    void pageIsSortedAndReportsTheTotal() {
        // WHY: Empty strings are ignored like null. Page 0, size 2, salary high to low.
        Page<Employee> page = repository.findAll(
                EmployeeSpecifications.matching("", "India", "", ""),
                PageRequest.of(0, 2, EmployeeSortField.SALARY.toSort(Sort.Direction.DESC)));

        // WHY: The two highest Indian salaries on page 1: Meera (2,000,000) then Ravi (1,400,000).
        assertThat(page.getContent()).extracting(Employee::getFirstName).containsExactly("Meera", "Ravi");
        // WHY: Total is all 3 matching rows, not just the 2 on this page, so there are 2 pages.
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // WHY: These lists fill the filter dropdowns. They must have no duplicates and be in A-Z order.
    @Test
    void distinctValuesAreSortedAlphabetically() {
        assertThat(repository.findDistinctCountries()).containsExactly("India", "United States");
        // WHY: "Engineering" appears 3 times in the data but only once here.
        assertThat(repository.findDistinctDepartments()).containsExactly("Data", "Engineering", "Product");
    }

    // WHY: On update, an employee keeps their own email. That must NOT count as a duplicate,
    // WHY: but using another person's email must.
    @Test
    void emailChecksIgnoreTheEmployeeBeingUpdated() {
        // WHY: Create case: Asha's email is already taken.
        assertThat(repository.existsByEmail("asha.rao@acme.com")).isTrue();
        // WHY: Update case for Asha herself: her own row is excluded, so no conflict.
        assertThat(repository.existsByEmailAndIdNot("asha.rao@acme.com", asha.getId())).isFalse();
        // WHY: Update case where Asha tries to take Ravi's email: conflict.
        assertThat(repository.existsByEmailAndIdNot("ravi.kumar@acme.com", asha.getId())).isTrue();
    }

    // WHY: Proves the unique constraint in the migration really exists. It is the last safety net
    // WHY: if two requests with the same email pass the service check at the same time.
    @Test
    void databaseRejectsADuplicateEmail() {
        // WHY: Same name gives the same email "asha.rao@acme.com" as the existing Asha.
        Employee copy = EmployeeTestData.employee("Asha", "Rao", "Data Analyst", "Data", "India", "900000", "INR");

        // WHY: saveAndFlush forces the INSERT now; without flush the error might not happen inside the test.
        // WHY: Spring translates the SQL error into DataIntegrityViolationException.
        assertThatThrownBy(() -> repository.saveAndFlush(copy)).isInstanceOf(DataIntegrityViolationException.class);
    }

    // WHY: Small helper so the search tests read like plain sentences: search text in, first names out.
    private List<String> search(String text) {
        return repository.findAll(EmployeeSpecifications.matching(text, null, null, null)).stream()
                .map(Employee::getFirstName)
                .toList();
    }
}
