// STUDY NOTES: A tiny record that holds one row of the headcount query. Hibernate creates it directly through "select new ...HeadcountRow(...)".
package com.acme.salary.repository;

/**
 * One row of the headcount query: how many employees work in a country.
 *
 * @param country       country name
 * @param employeeCount number of employees in that country
 */
// WHY: Long (object) because JPQL count() returns a Long and the constructor expression must match those types. It is database-shaped; the service maps it to the API DTO.
public record HeadcountRow(String country, Long employeeCount) {
}
