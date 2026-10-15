// STUDY NOTES: A typed holder for the app.seed.* settings in application.properties. The seeder reads it to know whether, how many and with which random seed to create demo employees.
package com.acme.salary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the demo data seed, bound from {@code app.seed.*} in application.properties.
 *
 * @param enabled       whether to seed on startup (only happens when the table is empty)
 * @param employeeCount how many employees to create
 * @param randomSeed    seed for the random generator, so every run produces the same data
 */
// WHY: prefix "app.seed" tells Spring to bind app.seed.enabled, app.seed.employee-count and app.seed.random-seed to the fields below (dashes map to camelCase).
// INTERVIEW: "A typed record is safer than @Value strings: a wrong type fails at start-up, and all seed settings are in one place."
@ConfigurationProperties(prefix = "app.seed")
// WHY: A record is immutable and needs no getters or constructor code; Spring fills it through the constructor.
public record SeedProperties(boolean enabled, int employeeCount, long randomSeed) {
}
