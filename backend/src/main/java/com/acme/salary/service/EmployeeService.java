// STUDY NOTES: The business logic for employees: list, get, create, update, delete and filter options. It enforces the rules (unique email, 404 for unknown ids) and owns the transactions.
package com.acme.salary.service;

import com.acme.salary.dto.EmployeeRequest;
import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.dto.FilterOptions;
import com.acme.salary.dto.PageResponse;
import com.acme.salary.entity.Employee;
import com.acme.salary.exception.DuplicateEmailException;
import com.acme.salary.exception.EmployeeNotFoundException;
import com.acme.salary.mapper.EmployeeMapper;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.EmployeeSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing employees: listing, reading, creating, updating and deleting,
 * and enforcing that every email is unique.
 */
// WHY: @Service registers this class as a Spring bean, so it can be injected into the controller.
@Service
// WHY: readOnly = true is the default for every method in the class; Hibernate can skip dirty checking for reads. Write methods override it below.
// INTERVIEW: "Read-only by default, and only the methods that change data opt in to a write transaction."
@Transactional(readOnly = true)
public class EmployeeService {

    // WHY: final + set in the constructor = the dependency is always present and never changes.
    private final EmployeeRepository repository;

    /**
     * @param repository employee data access
     */
    // WHY: Constructor injection (no @Autowired needed with one constructor). In unit tests we just pass a Mockito mock, no Spring needed.
    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns one page of employees matching the optional search and filters.
     *
     * @param search     text matched against name and email; may be blank
     * @param country    exact country; may be blank
     * @param jobTitle   exact job title; may be blank
     * @param department exact department; may be blank
     * @param pageable   page number, size and sort
     * @return the requested page
     */
    public PageResponse<EmployeeResponse> list(String search, String country, String jobTitle,
                                               String department, Pageable pageable) {
        // WHY: findAll(spec, pageable) runs one SELECT with WHERE/ORDER BY/LIMIT/OFFSET and one COUNT(*); only one page of rows is loaded.
        return PageResponse.from(repository
                .findAll(EmployeeSpecifications.matching(search, country, jobTitle, department), pageable)
                // WHY: Page.map converts each entity to a DTO while keeping the paging numbers.
                .map(EmployeeMapper::toResponse));
    }

    /**
     * @param id employee id
     * @return the employee
     * @throws EmployeeNotFoundException if the id does not exist
     */
    public EmployeeResponse get(Long id) {
        // WHY: Load or throw 404, then convert to the API shape.
        return EmployeeMapper.toResponse(findOrThrow(id));
    }

    /**
     * @param request validated employee data
     * @return the saved employee with its new id
     * @throws DuplicateEmailException if the email is already used
     */
    // WHY: Plain @Transactional (read-write) overrides the class-level readOnly because this method inserts a row.
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        // WHY: Normalise first (trim + lower-case), so the check compares the same form that will be stored.
        String email = EmployeeMapper.normaliseEmail(request.email());
        // WHY: Check first to give a clear 409 on the email field. The unique index is still the final guard if two requests race.
        if (repository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        // WHY: save() inserts the row; the returned entity has the new id and timestamps for the response.
        return EmployeeMapper.toResponse(repository.save(EmployeeMapper.toEntity(request)));
    }

    /**
     * Replaces all editable fields of an employee.
     *
     * @param id      employee id
     * @param request validated employee data
     * @return the updated employee
     * @throws EmployeeNotFoundException if the id does not exist
     * @throws DuplicateEmailException   if another employee already uses the email
     */
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        // WHY: 404 first if the employee does not exist.
        Employee employee = findOrThrow(id);
        String email = EmployeeMapper.normaliseEmail(request.email());
        // WHY: "IdNot" excludes this employee, so keeping your own email is allowed.
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }
        // WHY: Copy the new values into the loaded (managed) entity.
        EmployeeMapper.copyInto(request, employee);
        // WHY: saveAndFlush runs the UPDATE now, so @PreUpdate sets the new updatedAt before we build the response.
        return EmployeeMapper.toResponse(repository.saveAndFlush(employee));
    }

    /**
     * @param id employee id
     * @throws EmployeeNotFoundException if the id does not exist
     */
    @Transactional
    public void delete(Long id) {
        // WHY: Check existence so a wrong id gives a clear 404 instead of silently doing nothing.
        if (!repository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        repository.deleteById(id);
    }

    /**
     * @return the distinct countries, job titles and departments present in the data
     */
    public FilterOptions filterOptions() {
        // WHY: Three small DISTINCT queries; the dropdowns always show values that really exist.
        return new FilterOptions(
                repository.findDistinctCountries(),
                repository.findDistinctJobTitles(),
                repository.findDistinctDepartments());
    }

    // WHY: Shared helper so get and update handle a missing id in exactly the same way (404).
    private Employee findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }
}
