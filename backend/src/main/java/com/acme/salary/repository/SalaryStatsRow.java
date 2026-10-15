// STUDY NOTES: A tiny record that holds one row of a salary aggregate query. Shared by the "by country" and "by job title" queries because their columns are the same.
package com.acme.salary.repository;

import java.math.BigDecimal;

/**
 * One row of a salary aggregate query: statistics for one group (a country or a job title)
 * in one currency.
 *
 * @param groupKey      the value grouped by, e.g. "India" or "Software Engineer"
 * @param currency      ISO currency code shared by every salary in the group
 * @param minSalary     lowest salary in the group
 * @param maxSalary     highest salary in the group
 * @param averageSalary average salary as computed by the database (not yet rounded)
 * @param employeeCount number of employees in the group
 */
// WHY: The repository returns database-shaped rows; InsightsService turns them into API DTOs. This keeps the repository independent of the JSON format.
public record SalaryStatsRow(
        // WHY: A neutral name, because it holds a country in one query and a job title in the other.
        String groupKey,
        String currency,
        // WHY: min and max of a BigDecimal column come back as BigDecimal, so they stay exact.
        BigDecimal minSalary,
        BigDecimal maxSalary,
        // WHY: JPQL avg() always returns a Double; the service rounds it to 2 decimals with BigDecimal.
        Double averageSalary,
        // WHY: JPQL count() returns a Long.
        Long employeeCount) {
}
