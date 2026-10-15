// STUDY NOTES: Checks InsightsService, which turns the repository's aggregate rows into the response DTOs
// STUDY NOTES: (rounding the average, trimming the country). UNIT test with a Mockito mock repository: the
// STUDY NOTES: SQL itself is already tested in EmployeeRepositoryTest, so here we only test the Java mapping.
package com.acme.salary.service;

import com.acme.salary.dto.CountryHeadcount;
import com.acme.salary.dto.CountrySalaryStats;
import com.acme.salary.dto.JobTitleSalaryStats;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.HeadcountRow;
import com.acme.salary.repository.SalaryStatsRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// WHY: Turns on Mockito so @Mock and @InjectMocks are created before each test.
@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    // WHY: Fake repository so we can feed exact rows, including tricky numbers for rounding.
    @Mock
    private EmployeeRepository repository;

    // WHY: Real InsightsService with the mock repository passed into its constructor.
    @InjectMocks
    private InsightsService service;

    // WHY: The database average is a double with many decimals. Money in the API must have exactly
    // WHY: 2 decimals. This test proves the HALF_UP rounding.
    @Test
    void salaryByCountryRoundsAverageToTwoDecimals() {
        // WHY: Arrange: 1234.565 is the edge case. HALF_UP must round it UP to 1234.57.
        // WHY: (BigDecimal.valueOf reads the double as "1234.565", so the 5 is not lost.)
        when(repository.salaryStatsByCountry()).thenReturn(List.of(
                new SalaryStatsRow("India", "INR", new BigDecimal("1000.00"), new BigDecimal("2000.00"), 1234.565, 3L)));

        // WHY: Act.
        List<CountrySalaryStats> stats = service.salaryByCountry();

        // WHY: singleElement() asserts the list has exactly one item, then satisfies(...) checks its fields.
        assertThat(stats).singleElement().satisfies(row -> {
            // WHY: The group key becomes the "country" field of the DTO.
            assertThat(row.country()).isEqualTo("India");
            assertThat(row.currency()).isEqualTo("INR");
            assertThat(row.minSalary()).isEqualByComparingTo("1000.00");
            assertThat(row.maxSalary()).isEqualByComparingTo("2000.00");
            // WHY: isEqualTo (not isEqualByComparingTo) on purpose: BigDecimal.equals also checks the
            // WHY: scale, so this proves both the value 1234.57 AND exactly 2 decimal places.
            assertThat(row.averageSalary()).isEqualTo(new BigDecimal("1234.57"));
            assertThat(row.employeeCount()).isEqualTo(3);
        });
    }

    // WHY: Proves extra spaces in the ?country= parameter are removed before querying.
    @Test
    void salaryByJobTitleTrimsTheCountry() {
        // WHY: The stub only answers for exactly "India". With strict stubs, if the service passed
        // WHY: "  India " the stub would not match and the test would fail.
        when(repository.salaryStatsByJobTitle("India")).thenReturn(List.of(
                new SalaryStatsRow("Data Analyst", "INR", new BigDecimal("900000"), new BigDecimal("1100000"),
                        1000000.0, 2L)));

        // WHY: Act with spaces around the country name.
        List<JobTitleSalaryStats> stats = service.salaryByJobTitle("  India ");

        assertThat(stats).singleElement().satisfies(row -> {
            assertThat(row.jobTitle()).isEqualTo("Data Analyst");
            // WHY: A whole number still gets 2 decimals (1000000.00), so the JSON format is consistent.
            assertThat(row.averageSalary()).isEqualTo(new BigDecimal("1000000.00"));
        });
    }

    // WHY: Sorting is the database's job (ORDER BY count desc). The service must not re-sort or
    // WHY: lose that order when it maps rows to DTOs.
    @Test
    void headcountByCountryKeepsRepositoryOrder() {
        when(repository.headcountByCountry()).thenReturn(List.of(
                new HeadcountRow("United States", 30L), new HeadcountRow("India", 20L)));

        List<CountryHeadcount> headcount = service.headcountByCountry();

        // WHY: CountryHeadcount is a record, so we can compare whole objects; containsExactly checks the order too.
        assertThat(headcount).containsExactly(
                new CountryHeadcount("United States", 30), new CountryHeadcount("India", 20));
    }
}
