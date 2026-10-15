// STUDY NOTES: Builds the WHERE clause for the employee list from four optional inputs (search, country, job title, department), adding only the filters that are filled in.
package com.acme.salary.repository;

import com.acme.salary.entity.Employee;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the WHERE clause for the employee list from the optional search text and filters.
 * Blank values are ignored, so "no filters" means "all employees".
 */
// WHY: final with a private constructor: a utility class with only static methods.
// INTERVIEW: "Four optional filters make 16 combinations; a Specification adds one predicate per filled-in value, with no string concatenation."
public final class EmployeeSpecifications {

    // WHY: The backslash is our escape character for LIKE, so we can search for a literal % or _.
    private static final char LIKE_ESCAPE = '\\';

    private EmployeeSpecifications() {
    }

    /**
     * @param search     text matched (case-insensitive, contains) against first name, last
     *                   name, full name and email; may be blank
     * @param country    exact country to keep; may be blank
     * @param jobTitle   exact job title to keep; may be blank
     * @param department exact department to keep; may be blank
     * @return a specification combining every non-blank criterion with AND
     */
    public static Specification<Employee> matching(String search, String country, String jobTitle, String department) {
        // WHY: A Specification is a lambda: root = the Employee table, query = the whole query, cb = CriteriaBuilder that creates SQL conditions.
        return (root, query, cb) -> {
            // WHY: Collect one condition per filled-in filter, then AND them together at the end.
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(search)) {
                // WHY: "%text%" means "contains". Lower-case both sides for a case-insensitive match; escape wildcards the user typed.
                String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
                // WHY: Build "firstname lastname" in SQL so a search for "asha rao" matches the full name, not just one part.
                Expression<String> fullName = cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName")));
                // WHY: OR: the text may be in the full name or in the email. The 3rd argument tells SQL which escape character we used.
                predicates.add(cb.or(
                        cb.like(fullName, pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE)));
            }
            // WHY: Exact match (equal) for dropdown values; this can use the country index.
            if (hasText(country)) {
                predicates.add(cb.equal(root.get("country"), country.trim()));
            }
            if (hasText(jobTitle)) {
                predicates.add(cb.equal(root.get("jobTitle"), jobTitle.trim()));
            }
            if (hasText(department)) {
                predicates.add(cb.equal(root.get("department"), department.trim()));
            }
            // WHY: AND of all conditions. With an empty list, cb.and() is "always true", so no filters means all employees.
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    // WHY: null and "   " both mean "filter not used".
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Escapes LIKE wildcards so a search for "50%" matches the literal text.
     */
    private static String escapeLike(String value) {
        // WHY: Escape the escape character first, otherwise the next two replaces would be doubled up.
        return value
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                // WHY: % means "any text" in LIKE; escaped it means a real percent sign.
                .replace("%", LIKE_ESCAPE + "%")
                // WHY: _ means "any one character" in LIKE; escaped it means a real underscore.
                .replace("_", LIKE_ESCAPE + "_");
    }
}
