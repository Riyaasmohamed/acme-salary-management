// STUDY NOTES: The REST API for employees under /api/employees. It reads HTTP input, triggers validation, calls EmployeeService and picks the status code. No business rules here.
package com.acme.salary.controller;

import com.acme.salary.dto.EmployeeRequest;
import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.dto.EmployeeSortField;
import com.acme.salary.dto.FilterOptions;
import com.acme.salary.dto.PageResponse;
import com.acme.salary.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST endpoints for managing employees under {@code /api/employees}.
 */
// WHY: @RestController = @Controller + @ResponseBody: every return value is written as JSON, not as a page name.
@RestController
// WHY: Base URL for every method in this class; methods add only the rest of the path.
@RequestMapping("/api/employees")
// INTERVIEW: "Controllers are thin: they only know HTTP (URLs, params, status codes); the rules live in the service, which we unit-test without Spring."
public class EmployeeController {

    // WHY: final field filled by the constructor, so the controller always has its service.
    private final EmployeeService employeeService;

    /**
     * @param employeeService employee business logic
     */
    // WHY: Constructor injection: Spring passes the EmployeeService bean in; with one constructor no @Autowired is needed.
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Lists employees one page at a time, with optional search, filters and sort.
     *
     * @param search     text matched against name and email
     * @param country    exact country filter
     * @param jobTitle   exact job title filter
     * @param department exact department filter
     * @param page       zero-based page number
     * @param size       page size, 1 to 100
     * @param sortBy     column to sort by
     * @param direction  ASC or DESC
     * @return 200 with one page of employees
     */
    // WHY: GET /api/employees returns one page (status 200 by default).
    @GetMapping
    public PageResponse<EmployeeResponse> list(
            // WHY: required = false: the search and the three filters are optional; null means "not used".
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String department,
            // WHY: Page numbers start at 0; @Min(0) rejects negative pages with a 400.
            @RequestParam(defaultValue = "0") @Min(0) int page,
            // WHY: Default 20 rows; @Max(100) is a hard limit so nobody can download all 10,000 employees in one request.
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            // WHY: Spring converts the text to the enum; an unknown value like "password" becomes a 400, not a query.
            @RequestParam(defaultValue = "NAME") EmployeeSortField sortBy,
            // WHY: Spring's own enum, so only ASC or DESC are accepted.
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        // WHY: PageRequest bundles page, size and sort (with the id tie-breaker) into one Pageable for the repository.
        return employeeService.list(search, country, jobTitle, department,
                PageRequest.of(page, size, sortBy.toSort(direction)));
    }

    /**
     * @return 200 with the distinct countries, job titles and departments for the filters
     */
    // WHY: A fixed path; Spring prefers it over "/{id}", so "filter-options" is never read as an id.
    @GetMapping("/filter-options")
    public FilterOptions filterOptions() {
        return employeeService.filterOptions();
    }

    /**
     * @param id employee id
     * @return 200 with the employee, or 404
     */
    // WHY: {id} in the path is read by @PathVariable; a missing id becomes 404 through the exception handler.
    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    /**
     * @param request employee data
     * @return 201 with the created employee and a Location header, 400 or 409
     */
    @PostMapping
    // WHY: @RequestBody reads the JSON body; @Valid runs the Bean Validation rules on EmployeeRequest before the method body runs (400 if broken).
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = employeeService.create(request);
        // WHY: Build the URL of the new resource, e.g. /api/employees/42, from the current request URL plus the new id.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        // WHY: 201 Created plus a Location header is the REST standard answer for "a new resource was made".
        // INTERVIEW: "POST returns 201 with Location, PUT returns 200, DELETE returns 204 - each status code tells the client exactly what happened."
        return ResponseEntity.created(location).body(created);
    }

    /**
     * @param id      employee id
     * @param request new employee data
     * @return 200 with the updated employee, 400, 404 or 409
     */
    // WHY: PUT because the client sends the full employee and replaces all editable fields.
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    /**
     * @param id employee id
     * @return 204, or 404
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        // WHY: 204 No Content: the delete worked and there is nothing to send back.
        return ResponseEntity.noContent().build();
    }
}
