// STUDY NOTES: Output DTO for the "salary by job title in a country" insight: min, max, average and headcount for one job title.
package com.acme.salary.dto;

import java.math.BigDecimal;

/**
 * Salary statistics for one job title inside a country.
 *
 * @param jobTitle      job title
 * @param currency      ISO currency code of all salaries in this row
 * @param minSalary     lowest salary
 * @param maxSalary     highest salary
 * @param averageSalary average salary rounded to two decimals
 * @param employeeCount number of employees with this title
 */
// WHY: Same shape as CountrySalaryStats, but the group key is the job title. A separate record keeps the JSON field name meaningful ("jobTitle").
public record JobTitleSalaryStats(
        // WHY: The group key; the country is already fixed by the request parameter.
        String jobTitle,
        // WHY: Kept in the row so every number has a clear currency and currencies are never mixed.
        String currency,
        // WHY: BigDecimal for exact money values.
        BigDecimal minSalary,
        BigDecimal maxSalary,
        // WHY: Rounded to 2 decimals (HALF_UP) by InsightsService.
        BigDecimal averageSalary,
        // WHY: long because SQL COUNT returns a long.
        long employeeCount) {
}
