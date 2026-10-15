// STUDY NOTES: Checks the HTTP contract of InsightsController: the three /api/insights endpoints return
// STUDY NOTES: the right JSON, and the required "country" parameter is validated. SLICE test (@WebMvcTest)
// STUDY NOTES: with a mocked InsightsService: fast, no database, focused only on the web layer.
package com.acme.salary.controller;

import com.acme.salary.dto.CountryHeadcount;
import com.acme.salary.dto.CountrySalaryStats;
import com.acme.salary.dto.JobTitleSalaryStats;
import com.acme.salary.service.InsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the HTTP contract of {@link InsightsController} with a mocked service.
 */
// WHY: Starts only the MVC layer for InsightsController (plus JSON, validation and the exception handler).
// INTERVIEW: The controller is thin, so a slice test with a mocked service is enough to prove routing,
// INTERVIEW: JSON field names and parameter validation.
@WebMvcTest(InsightsController.class)
class InsightsControllerTest {

    // WHY: Sends fake HTTP requests through Spring MVC without a real server.
    @Autowired
    private MockMvc mockMvc;

    // WHY: Replaces the real service in the Spring context with a mock that we control.
    @MockitoBean
    private InsightsService insightsService;

    // WHY: Proves the endpoint URL and that the DTO fields appear in the JSON with the expected names.
    @Test
    void salaryByCountryReturnsStatsPerCountry() throws Exception {
        // WHY: Arrange: the mock service returns one India row.
        when(insightsService.salaryByCountry()).thenReturn(List.of(new CountrySalaryStats("India", "INR",
                new BigDecimal("1000000.00"), new BigDecimal("2000000.00"), new BigDecimal("1466666.67"), 3)));

        // WHY: Act: GET the endpoint.
        mockMvc.perform(get("/api/insights/salary-by-country"))
                .andExpect(status().isOk())
                // WHY: The response is a JSON array, so $[0] is the first element.
                .andExpect(jsonPath("$[0].country").value("India"))
                // WHY: BigDecimal is written as a JSON number (not a string), so the UI can use it directly.
                .andExpect(jsonPath("$[0].averageSalary").value(1466666.67))
                .andExpect(jsonPath("$[0].employeeCount").value(3));
    }

    // WHY: Proves the ?country= parameter is passed to the service.
    @Test
    void salaryByJobTitleReturnsStatsForTheCountry() throws Exception {
        // WHY: The stub only answers for "India", so a 200 with data proves the value was passed through.
        when(insightsService.salaryByJobTitle("India")).thenReturn(List.of(new JobTitleSalaryStats(
                "Product Manager", "INR", new BigDecimal("2000000"), new BigDecimal("2000000"),
                new BigDecimal("2000000.00"), 1)));

        mockMvc.perform(get("/api/insights/salary-by-job-title").param("country", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobTitle").value("Product Manager"));
    }

    // WHY: country is required and must not be blank. Two cases: missing completely, and only a space.
    @Test
    void salaryByJobTitleRequiresACountry() throws Exception {
        // WHY: Missing parameter -> 400 (Spring rejects a missing required @RequestParam).
        mockMvc.perform(get("/api/insights/salary-by-job-title")).andExpect(status().isBadRequest());
        // WHY: A blank value " " passes "required" but fails @NotBlank -> 400 with an error for "country".
        mockMvc.perform(get("/api/insights/salary-by-job-title").param("country", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.country").exists());
    }

    // WHY: Proves the headcount endpoint URL and JSON field names.
    @Test
    void headcountByCountryReturnsCounts() throws Exception {
        when(insightsService.headcountByCountry()).thenReturn(List.of(new CountryHeadcount("India", 3)));

        mockMvc.perform(get("/api/insights/headcount-by-country"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country").value("India"))
                .andExpect(jsonPath("$[0].employeeCount").value(3));
    }
}
