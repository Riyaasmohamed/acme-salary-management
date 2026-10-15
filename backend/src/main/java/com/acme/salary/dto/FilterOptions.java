// STUDY NOTES: Output DTO with the values for the three filter dropdowns in the UI (country, job title, department).
package com.acme.salary.dto;

import java.util.List;

/**
 * Values that currently exist in the data, used to fill the filter dropdowns in the UI.
 *
 * @param countries   distinct countries, alphabetically
 * @param jobTitles   distinct job titles, alphabetically
 * @param departments distinct departments, alphabetically
 */
// WHY: The lists come from SELECT DISTINCT on the real data, so the dropdowns only offer values that exist; nothing is hard-coded in the UI.
public record FilterOptions(List<String> countries, List<String> jobTitles, List<String> departments) {
}
