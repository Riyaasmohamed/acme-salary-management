// STUDY NOTES: Output DTO for the "salary by country" insight: min, max, average and headcount for one country in one currency.
package com.acme.salary.dto;

import java.math.BigDecimal;

/**
 * Salary statistics for one country (and currency).
 *
 * @param country       country name
 * @param currency      ISO currency code of all salaries in this row
 * @param minSalary     lowest salary
 * @param maxSalary     highest salary
 * @param averageSalary average salary rounded to two decimals
 * @param employeeCount number of employees
 */
// WHY: A record keeps the response immutable and short; the JSON field names come from the component names.
public record CountrySalaryStats(
        // WHY: The group key: one row per country.
        String country,
        // WHY: Currency is part of the row because we never average INR and USD together; every number in the row is in this one currency.
        // INTERVIEW: "Stats are grouped by country AND currency, so no number mixes currencies; conversion needs live rates and is out of scope."
        String currency,
        // WHY: BigDecimal, not double, because money must be exact (double cannot store 0.10 exactly).
        BigDecimal minSalary,
        BigDecimal maxSalary,
        // WHY: Already rounded to 2 decimals (HALF_UP) by InsightsService before it gets here.
        BigDecimal averageSalary,
        // WHY: long because SQL COUNT returns a long.
        long employeeCount) {
}
