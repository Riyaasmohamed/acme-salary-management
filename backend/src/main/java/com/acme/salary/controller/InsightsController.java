// STUDY NOTES: Read-only REST endpoints under /api/insights that answer the HR Manager's questions about pay (salary by country, by job title, headcount).
package com.acme.salary.controller;

import com.acme.salary.dto.CountryHeadcount;
import com.acme.salary.dto.CountrySalaryStats;
import com.acme.salary.dto.JobTitleSalaryStats;
import com.acme.salary.service.InsightsService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoints under {@code /api/insights} that answer questions about pay.
 */
// WHY: @RestController so the returned lists are written as JSON arrays.
@RestController
// WHY: Its own base URL, separate from /api/employees, because reports are a different concern from CRUD.
@RequestMapping("/api/insights")
public class InsightsController {

    // WHY: Constructor-injected service; the controller holds no logic of its own.
    private final InsightsService insightsService;

    /**
     * @param insightsService salary statistics logic
     */
    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    /**
     * @return 200 with min, max, average salary and headcount per country
     */
    // WHY: Only GET mappings in this class: insights never change data.
    @GetMapping("/salary-by-country")
    public List<CountrySalaryStats> salaryByCountry() {
        return insightsService.salaryByCountry();
    }

    /**
     * @param country exact country name
     * @return 200 with salary statistics per job title in that country, or 400 if blank
     */
    @GetMapping("/salary-by-job-title")
    // WHY: @RequestParam with no "required = false" makes country mandatory; @NotBlank also rejects "?country=" or spaces with a 400 and a field message.
    public List<JobTitleSalaryStats> salaryByJobTitle(@RequestParam @NotBlank String country) {
        return insightsService.salaryByJobTitle(country);
    }

    /**
     * @return 200 with the number of employees per country
     */
    @GetMapping("/headcount-by-country")
    public List<CountryHeadcount> headcountByCountry() {
        return insightsService.headcountByCountry();
    }
}
