// STUDY NOTES: Output DTO for the "headcount by country" insight. One instance becomes one JSON object like {"country":"India","employeeCount":2480}.
package com.acme.salary.dto;

/**
 * Number of employees working in one country.
 *
 * @param country       country name
 * @param employeeCount number of employees
 */
// WHY: A record because it is plain read-only data; Java writes the constructor, accessors, equals and toString for us. long matches what SQL COUNT returns.
public record CountryHeadcount(String country, long employeeCount) {
}
