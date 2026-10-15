// STUDY NOTES: Answers the HR Manager's three pay questions. The database does all the maths; this class only maps rows to API DTOs and rounds the averages.
package com.acme.salary.service;

import com.acme.salary.dto.CountryHeadcount;
import com.acme.salary.dto.CountrySalaryStats;
import com.acme.salary.dto.JobTitleSalaryStats;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryStatsRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Answers the HR Manager's questions about pay. The database does the aggregation; this
 * class only shapes the rows into API responses and rounds averages to two decimals.
 */
// WHY: A separate service keeps reporting logic apart from employee CRUD logic.
@Service
// WHY: Every method here only reads, so the whole class is read-only.
@Transactional(readOnly = true)
public class InsightsService {

    // WHY: Constructor-injected, final dependency, the same pattern as EmployeeService.
    private final EmployeeRepository repository;

    /**
     * @param repository employee data access
     */
    public InsightsService(EmployeeRepository repository) {
        this.repository = repository;
    }

    /**
     * @return min, max, average salary and headcount per country and currency
     */
    public List<CountrySalaryStats> salaryByCountry() {
        // WHY: The query returns about 8 rows; the stream only renames fields and rounds the average. No loop over employees.
        return repository.salaryStatsByCountry().stream()
                .map(row -> new CountrySalaryStats(row.groupKey(), row.currency(), row.minSalary(),
                        row.maxSalary(), roundAverage(row), row.employeeCount()))
                // WHY: toList() (Java 16+) returns an unmodifiable list.
                .toList();
    }

    /**
     * @param country exact country name
     * @return salary statistics per job title in that country, highest average first
     */
    public List<JobTitleSalaryStats> salaryByJobTitle(String country) {
        // WHY: trim() so " India " still matches; the controller already rejects a blank country.
        return repository.salaryStatsByJobTitle(country.trim()).stream()
                .map(row -> new JobTitleSalaryStats(row.groupKey(), row.currency(), row.minSalary(),
                        row.maxSalary(), roundAverage(row), row.employeeCount()))
                .toList();
    }

    /**
     * @return number of employees per country, largest first
     */
    public List<CountryHeadcount> headcountByCountry() {
        // WHY: Maps each database row to the API record; ordering is already done in SQL.
        return repository.headcountByCountry().stream()
                .map(row -> new CountryHeadcount(row.country(), row.employeeCount()))
                .toList();
    }

    /**
     * The database returns AVG as a floating-point number; money is shown with two decimals.
     */
    // INTERVIEW: "AVG comes back as a double, so we round once, HALF_UP to 2 decimals, at the last step, into a BigDecimal."
    private static BigDecimal roundAverage(SalaryStatsRow row) {
        // WHY: BigDecimal.valueOf uses the double's shortest text form, which avoids long binary tails; HALF_UP is normal school rounding (2.345 -> 2.35).
        return BigDecimal.valueOf(row.averageSalary()).setScale(2, RoundingMode.HALF_UP);
    }
}
