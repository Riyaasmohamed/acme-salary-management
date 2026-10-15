// STUDY NOTES: An enum listing the only columns the employee list may be sorted by. It turns a value like SALARY into a Spring Data Sort.
package com.acme.salary.dto;

import org.springframework.data.domain.Sort;

/**
 * Columns the employee list can be sorted by. An explicit list (instead of accepting any
 * property name) keeps sorting limited to indexed or cheap columns and avoids errors for
 * unknown fields.
 */
// WHY: An enum is a whitelist. Spring turns ?sortBy=SALARY into this enum, and any other value fails as a 400 before reaching the database.
// INTERVIEW: "Clients cannot sort by any random or non-existent property; an unknown value is a clean 400, never a 500."
public enum EmployeeSortField {
    // WHY: Sort by name means last name first, then first name, the way people expect a name list.
    NAME("lastName", "firstName"),
    // WHY: The values are entity property names (Java names), not SQL column names, because Spring Data sorts on the entity.
    SALARY("salary"),
    HIRE_DATE("hireDate");

    // WHY: Each constant keeps its entity property names; final because they never change.
    private final String[] properties;

    // WHY: Varargs (String...) lets NAME have two properties and the others one.
    EmployeeSortField(String... properties) {
        this.properties = properties;
    }

    /**
     * Builds the sort, adding id as a tie-breaker so rows with equal values keep a stable
     * order across pages.
     *
     * @param direction ascending or descending
     * @return the sort to pass to the repository
     */
    public Sort toSort(Sort.Direction direction) {
        // WHY: Sort by the chosen properties, then by id ascending. Many employees share a salary; without the id tie-breaker they could jump between pages.
        return Sort.by(direction, properties).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
