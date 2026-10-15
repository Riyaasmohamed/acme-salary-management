// STUDY NOTES: Checks the HTTP contract of EmployeeController: URLs, status codes (200/201/204/400/404/409),
// STUDY NOTES: JSON shape, validation error bodies and how query parameters become a Pageable. SLICE test
// STUDY NOTES: (@WebMvcTest): only the web layer starts (controller, JSON, validation, GlobalExceptionHandler).
// STUDY NOTES: The service is a mock, so no database is needed. This tests "what the client sees" quickly.
package com.acme.salary.controller;

import com.acme.salary.dto.EmployeeRequest;
import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.dto.PageResponse;
import com.acme.salary.exception.DuplicateEmailException;
import com.acme.salary.exception.EmployeeNotFoundException;
import com.acme.salary.service.EmployeeService;
import com.acme.salary.support.EmployeeTestData;
// WHY: Jackson's ObjectMapper turns Java objects into JSON text for the request body.
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
// WHY: @MockitoBean (Spring Framework 6.2+) puts a Mockito mock into the Spring context, replacing the real bean.
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// WHY: MockMvc sends fake HTTP requests through the real Spring MVC pipeline, without starting a server.
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
// WHY: Request builders: get/post/put/delete create the fake HTTP request.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
// WHY: Result matchers: check headers, JSON fields (jsonPath) and the status code of the response.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the HTTP contract of {@link EmployeeController}: status codes, JSON shape and error
 * bodies. The service is mocked, so no database is involved.
 */
// WHY: @WebMvcTest(EmployeeController.class) loads only this controller plus MVC infrastructure
// WHY: (Jackson, Bean Validation, @RestControllerAdvice). Services and repositories are NOT loaded.
// INTERVIEW: @WebMvcTest vs @SpringBootTest: the slice is fast and tests only the HTTP layer
// INTERVIEW: (routing, JSON, validation, error mapping). Business rules are tested in EmployeeServiceTest.
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    // WHY: Auto-configured by @WebMvcTest; we use it to perform requests and check responses.
    @Autowired
    private MockMvc mockMvc;

    // WHY: The same ObjectMapper Spring uses, so dates like LocalDate are written the same way the real app reads them.
    @Autowired
    private ObjectMapper objectMapper;

    // WHY: The controller needs an EmployeeService bean. @MockitoBean adds a mock one so we control its answers.
    @MockitoBean
    private EmployeeService employeeService;

    // WHY: REST rule for create: 201 Created, a Location header pointing at the new resource, and the body.
    @Test
    void createReturns201WithLocationHeader() throws Exception {
        // WHY: Arrange: whatever request comes in, the mock service "creates" employee 7.
        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(response(7L));

        // WHY: Act: POST valid JSON. contentType tells Spring the body is JSON.
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(json(EmployeeTestData.validRequest())))
                // WHY: Assert: HTTP 201.
                .andExpect(status().isCreated())
                // WHY: MockMvc uses "http://localhost" as the host; the path ends with the new id.
                .andExpect(header().string("Location", "http://localhost/api/employees/7"))
                // WHY: jsonPath("$.id") reads the "id" field from the JSON body ($ = the root).
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("asha.rao@acme.com"));
    }

    // WHY: Proves @Valid works and the error body lists one message PER FIELD, so the UI can show
    // WHY: each message under the right input.
    @Test
    void createReturns400WithAMessagePerInvalidField() throws Exception {
        // WHY: Arrange: several broken fields at once: blank name, bad email, negative salary,
        // WHY: unknown currency, hire date in the future (tomorrow).
        EmployeeRequest invalid = new EmployeeRequest("", "Rao", "not-an-email", "Engineer", "Engineering",
                "India", new BigDecimal("-1"), "XYZ", LocalDate.now().plusDays(1));

        // WHY: No when(...) here: validation fails BEFORE the controller method runs, so the service is never called.
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest())
                // WHY: The ProblemDetail title set by GlobalExceptionHandler.
                .andExpect(jsonPath("$.title").value("Validation failed"))
                // WHY: Exact default messages from @NotBlank and @Positive.
                .andExpect(jsonPath("$.errors.firstName").value("must not be blank"))
                // WHY: exists() only checks the field has an error; the exact text is less important.
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.salary").value("must be greater than 0"))
                // WHY: Proves our custom @ValidCurrency is wired into validation.
                .andExpect(jsonPath("$.errors.currency").exists())
                // WHY: Proves @PastOrPresent blocks future hire dates.
                .andExpect(jsonPath("$.errors.hireDate").exists());
    }

    // WHY: Broken JSON must give a clean 400 (client error), not a 500 (server error).
    @Test
    void createReturns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    // WHY: Proves the DuplicateEmailException from the service becomes HTTP 409 Conflict with a helpful body.
    @Test
    void createReturns409WhenEmailIsTaken() throws Exception {
        // WHY: thenThrow makes the mock service throw, like the real service does for a taken email.
        when(employeeService.create(any(EmployeeRequest.class)))
                .thenThrow(new DuplicateEmailException("asha.rao@acme.com"));

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(json(EmployeeTestData.validRequest())))
                .andExpect(status().isConflict())
                // WHY: The exception message is passed to the client as "detail".
                .andExpect(jsonPath("$.detail").value("An employee with email asha.rao@acme.com already exists"))
                // WHY: A field-level error under "email" lets the form highlight the email input.
                .andExpect(jsonPath("$.errors.email").value("This email is already used by another employee"));
    }

    // WHY: Proves EmployeeNotFoundException becomes HTTP 404 with a clear message.
    @Test
    void getReturns404ForUnknownId() throws Exception {
        when(employeeService.get(99L)).thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Employee 99 was not found"));
    }

    // WHY: Proves the controller turns URL parameters into the right service call: filters, page,
    // WHY: size and sort. A mistake here would show the wrong page or ignore a filter in the UI.
    @Test
    void listTurnsQueryParametersIntoSearchFiltersAndPageRequest() throws Exception {
        // WHY: eq(null) for jobTitle because that parameter is not sent. The Pageable is accepted with any().
        // WHY: The fake page says page 2, size 50, 101 rows in total, 3 pages.
        when(employeeService.list(eq("rao"), eq("India"), eq(null), eq("Engineering"), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(response(7L)), 2, 50, 101, 3));

        // WHY: Act: GET with query parameters, like the Angular list page sends.
        mockMvc.perform(get("/api/employees").param("search", "rao").param("country", "India")
                        .param("department", "Engineering").param("page", "2").param("size", "50")
                        .param("sortBy", "SALARY").param("direction", "DESC"))
                .andExpect(status().isOk())
                // WHY: $.content[0].id = id of the first row in the page.
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.totalElements").value(101));

        // WHY: Capture the Pageable the controller built and check every part of it.
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeService).list(eq("rao"), eq("India"), eq(null), eq("Engineering"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        // WHY: SALARY maps to the "salary" property, plus "id" as a tie-breaker so rows with the same
        // WHY: salary always come in the same order (stable paging, no duplicates across pages).
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "salary").and(Sort.by("id")));
    }

    // WHY: Protects the server: @Max(100) on size stops a client asking for a huge page.
    @Test
    void listRejectsPageSizeAbove100() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "500"))
                .andExpect(status().isBadRequest())
                // WHY: The error is reported under the parameter name "size".
                .andExpect(jsonPath("$.errors.size").exists());
    }

    // WHY: sortBy is an enum (NAME, SALARY, HIRE_DATE), so clients cannot sort by any random column.
    // WHY: An unknown value like "password" fails to convert and gives 400.
    @Test
    void listRejectsUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/employees").param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    // WHY: Happy path for update: PUT returns 200 with the updated employee.
    @Test
    void updateReturns200WithTheUpdatedEmployee() throws Exception {
        // WHY: eq(7L) proves the id comes from the URL path.
        when(employeeService.update(eq(7L), any(EmployeeRequest.class))).thenReturn(response(7L));

        mockMvc.perform(put("/api/employees/7").contentType(MediaType.APPLICATION_JSON)
                        .content(json(EmployeeTestData.validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    // WHY: REST rule for delete: 204 No Content (success, empty body).
    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/employees/7")).andExpect(status().isNoContent());

        // WHY: The controller really asked the service to delete id 7.
        verify(employeeService).delete(7L);
    }

    // WHY: Helper: turns a request object into a JSON string for the request body.
    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // WHY: Helper: builds a fake service response with a fixed timestamp (no Instant.now()) so the test is repeatable.
    private static EmployeeResponse response(Long id) {
        EmployeeRequest r = EmployeeTestData.validRequest();
        Instant created = Instant.parse("2026-01-01T10:00:00Z");
        return new EmployeeResponse(id, r.firstName(), r.lastName(), r.email(), r.jobTitle(), r.department(),
                r.country(), r.salary(), r.currency(), r.hireDate(), created, created);
    }
}
