// STUDY NOTES: Our own page format for list responses: the rows plus the numbers the Angular paginator needs.
package com.acme.salary.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * One page of results plus the numbers the UI paginator needs. Used instead of returning
 * Spring's {@link Page} directly, whose JSON shape is not a stable contract.
 *
 * @param content       items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements number of items across all pages
 * @param totalPages    number of pages
 * @param <T>           item type
 */
// WHY: Generic <T> so the same record can hold a page of any item type (here EmployeeResponse).
// INTERVIEW: "Spring Data warns that PageImpl's JSON is not stable, so we return exactly five fields that we control."
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * @param page a Spring Data page
     * @param <T>  item type
     * @return the same data in the API's page format
     */
    // WHY: A static factory keeps the Page-to-PageResponse conversion in one place, so the service just calls PageResponse.from(...).
    public static <T> PageResponse<T> from(Page<T> page) {
        // WHY: Copies the rows and the four paging numbers out of Spring's Page object.
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
