// STUDY NOTES: Checks the business rules in EmployeeService: normalising input, unique email on create
// STUDY NOTES: and update, "not found" errors, delete, paging and filter options. UNIT test with Mockito:
// STUDY NOTES: the repository is mocked, so there is no database and no Spring. Each test runs in milliseconds
// STUDY NOTES: and fails only if the SERVICE logic is wrong. The real SQL is covered by EmployeeRepositoryTest.
package com.acme.salary.service;

import com.acme.salary.dto.EmployeeRequest;
import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.dto.FilterOptions;
import com.acme.salary.dto.PageResponse;
import com.acme.salary.entity.Employee;
import com.acme.salary.exception.DuplicateEmailException;
import com.acme.salary.exception.EmployeeNotFoundException;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.support.EmployeeTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// WHY: ArgumentCaptor lets us look inside the object the service passed to the mock.
import org.mockito.ArgumentCaptor;
// WHY: Needed for a typed any() matcher on the generic Specification argument.
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// WHY: Enables Mockito in JUnit 5: creates @Mock fields and injects them into @InjectMocks before each test.
// WHY: Strict stubs are on by default, so a when(...) the code never uses (or uses with other arguments) fails the test.
// INTERVIEW: No Spring context here on purpose. The service is plain Java with constructor injection, so a
// INTERVIEW: mock repository is all it needs. This keeps the test fast and focused on business rules.
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    // WHY: Fake repository. We decide what it returns, and we can verify what the service asked it to do.
    @Mock
    private EmployeeRepository repository;

    // WHY: Mockito creates a real EmployeeService and passes the mock repository into its constructor.
    @InjectMocks
    private EmployeeService service;

    // WHY: Proves create() cleans the input before saving: trims names, lower-cases the email,
    // WHY: upper-cases the currency, and returns the id the database gave.
    @Test
    void createSavesNormalisedEmployeeWhenEmailIsFree() {
        // WHY: Arrange: messy input on purpose (spaces, mixed case, lower-case "inr").
        EmployeeRequest request = new EmployeeRequest(" Asha ", "Rao", " Asha.Rao@ACME.com", "Software Engineer",
                "Engineering", "India", new BigDecimal("1500000"), "inr", EmployeeTestData.validRequest().hireDate());
        // WHY: Stubbed with the CLEAN email. With strict stubs, if the service checked the raw email instead,
        // WHY: the stub would not match and the test would fail. So this also proves the email is normalised first.
        when(repository.existsByEmail("asha.rao@acme.com")).thenReturn(false);
        // WHY: thenAnswer runs code at call time: it takes the entity the service passed in and gives it
        // WHY: id 7, like the database would. any(Employee.class) matches whatever entity is passed.
        when(repository.save(any(Employee.class))).thenAnswer(call -> withId(call.getArgument(0), 7L));

        // WHY: Act.
        EmployeeResponse response = service.create(request);

        // WHY: Assert: capture the entity sent to save() and check it was normalised.
        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFirstName()).isEqualTo("Asha");
        assertThat(saved.getValue().getEmail()).isEqualTo("asha.rao@acme.com");
        assertThat(saved.getValue().getCurrency()).isEqualTo("INR");
        // WHY: The response carries the generated id, so the UI knows which row was created.
        assertThat(response.id()).isEqualTo(7L);
    }

    // WHY: Business rule: emails are unique. A clear error (later HTTP 409) is better than a raw database crash.
    @Test
    void createRejectsEmailThatIsAlreadyUsed() {
        // WHY: Arrange: the email is already taken.
        when(repository.existsByEmail("asha.rao@acme.com")).thenReturn(true);

        // WHY: Act + Assert: the call throws our domain exception, and the message names the email
        // WHY: so the user knows what to fix.
        assertThatThrownBy(() -> service.create(EmployeeTestData.validRequest()))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("asha.rao@acme.com");
        // WHY: Nothing must be saved when validation fails.
        verify(repository, never()).save(any());
    }

    // WHY: An unknown id must give a clear "not found" error (mapped to HTTP 404), not a NullPointerException.
    @Test
    void getThrowsNotFoundForUnknownId() {
        // WHY: findById returns Optional; empty means "no such row".
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(EmployeeNotFoundException.class);
    }

    // WHY: Proves update changes the EXISTING entity (same object, same id) with the new values.
    @Test
    void updateOverwritesFieldsOfExistingEmployee() {
        // WHY: Arrange: an employee already stored with id 7.
        Employee existing = withId(EmployeeTestData.employee("Asha", "Rao", "Software Engineer", "Engineering",
                "India", "1500000", "INR"), 7L);
        EmployeeRequest request = EmployeeTestData.validRequestWithEmail("asha.rao@acme.com");
        // WHY: The change: a promotion with a new job title and a higher salary. She keeps her own email.
        EmployeeRequest raise = new EmployeeRequest(request.firstName(), request.lastName(), request.email(),
                "Senior Software Engineer", request.department(), request.country(), new BigDecimal("1800000"),
                request.currency(), request.hireDate());
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        // WHY: Her own email is not a conflict, because the check excludes her own id (7).
        when(repository.existsByEmailAndIdNot("asha.rao@acme.com", 7L)).thenReturn(false);
        // WHY: Stubbed with the SAME "existing" object. If the service saved a new object instead,
        // WHY: this stub would not match and the test would fail.
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        // WHY: Act.
        EmployeeResponse response = service.update(7L, raise);

        // WHY: Assert: the response shows the new values.
        assertThat(response.jobTitle()).isEqualTo("Senior Software Engineer");
        // WHY: isEqualByComparingTo compares BigDecimal by value, ignoring scale.
        assertThat(response.salary()).isEqualByComparingTo("1800000");
    }

    // WHY: On update you must not take another person's email.
    @Test
    void updateRejectsEmailUsedByAnotherEmployee() {
        Employee existing = withId(EmployeeTestData.employee("Asha", "Rao", "Software Engineer", "Engineering",
                "India", "1500000", "INR"), 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        // WHY: Another employee (not id 7) already uses "taken@acme.com".
        when(repository.existsByEmailAndIdNot("taken@acme.com", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(7L, EmployeeTestData.validRequestWithEmail("taken@acme.com")))
                .isInstanceOf(DuplicateEmailException.class);
        // WHY: The change must not be saved.
        verify(repository, never()).saveAndFlush(any());
    }

    // WHY: Updating an id that does not exist must give "not found", not create a new row.
    @Test
    void updateThrowsNotFoundForUnknownId() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, EmployeeTestData.validRequest()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // WHY: Happy path for delete: the service checks the row exists, then deletes it by id.
    @Test
    void deleteRemovesExistingEmployee() {
        when(repository.existsById(7L)).thenReturn(true);

        service.delete(7L);

        // WHY: delete() returns nothing, so we prove the effect by verifying the call on the mock.
        verify(repository).deleteById(7L);
    }

    // WHY: Deleting an unknown id must give "not found" (HTTP 404) and must not call deleteById.
    @Test
    void deleteThrowsNotFoundForUnknownId() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    // WHY: Proves list() maps entities to response DTOs and copies the paging numbers into our PageResponse.
    @Test
    void listReturnsMappedPageWithTotals() {
        // WHY: Page index 1 (the second page), 2 rows per page.
        Pageable pageable = PageRequest.of(1, 2);
        Employee employee = withId(EmployeeTestData.employee("Asha", "Rao", "Software Engineer", "Engineering",
                "India", "1500000", "INR"), 7L);
        // WHY: The Specification is built inside the service, so we cannot create an equal one here.
        // WHY: We accept any Specification but require the exact pageable with eq(...).
        // WHY: (When one argument uses a matcher, all arguments must use matchers.)
        // WHY: PageImpl with total 5 is a fake page: 1 row shown, 5 rows in total.
        when(repository.findAll(ArgumentMatchers.<Specification<Employee>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 5));

        PageResponse<EmployeeResponse> page = service.list("asha", "India", null, null, pageable);

        // WHY: The entity became an EmployeeResponse with the right email.
        assertThat(page.content()).extracting(EmployeeResponse::email).containsExactly("asha.rao@acme.com");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(5);
        // WHY: 5 rows with 2 per page = 3 pages. The UI paginator needs this number.
        assertThat(page.totalPages()).isEqualTo(3);
    }

    // WHY: Proves the dropdown values are passed straight through from the three distinct-value queries.
    @Test
    void filterOptionsComesFromDistinctValues() {
        when(repository.findDistinctCountries()).thenReturn(List.of("India", "Japan"));
        when(repository.findDistinctJobTitles()).thenReturn(List.of("Data Analyst"));
        when(repository.findDistinctDepartments()).thenReturn(List.of("Data"));

        FilterOptions options = service.filterOptions();

        // WHY: Each list lands in the right field (countries not mixed up with departments, for example).
        assertThat(options.countries()).containsExactly("India", "Japan");
        assertThat(options.jobTitles()).containsExactly("Data Analyst");
        assertThat(options.departments()).containsExactly("Data");
    }

    // WHY: Small helper to fake a saved entity: in real life the database sets the id.
    private static Employee withId(Employee employee, Long id) {
        employee.setId(id);
        return employee;
    }
}
