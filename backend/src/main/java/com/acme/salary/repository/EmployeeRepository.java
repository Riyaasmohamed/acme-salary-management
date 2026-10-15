// STUDY NOTES: The Spring Data JPA repository for employees. We only declare methods; Spring writes the implementation at start-up. All SQL for employees lives here.
package com.acme.salary.repository;

import com.acme.salary.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Database access for employees: CRUD, paged search (via specifications) and the
 * aggregate queries behind the insights page. All statistics are computed by the
 * database with GROUP BY, never by loading employees into memory.
 */
// WHY: JpaRepository<Employee, Long> gives save, findById, deleteById, existsById, paging and sorting for free (Long is the id type).
// WHY: JpaSpecificationExecutor adds findAll(Specification, Pageable), used for the list with optional filters.
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /**
     * @param email email in lower case
     * @return true if any employee already uses this email
     */
    // WHY: A "derived query": Spring reads the method name and builds the SQL (WHERE email = ?). The unique index makes it a 1-row lookup. Used on create.
    boolean existsByEmail(String email);

    /**
     * @param email email in lower case
     * @param id    id of the employee being updated
     * @return true if a different employee already uses this email
     */
    // WHY: Used on update: "AndIdNot" ignores the employee being edited, so they can keep their own email.
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Min, max, average salary and headcount per country. Grouped by currency as well so
     * an average never mixes two currencies.
     *
     * @return one row per country and currency, ordered by country
     */
    // WHY: @Query holds JPQL (it uses entity and field names, not table names). "select new ...SalaryStatsRow(...)" puts each result row straight into a record.
    // WHY: GROUP BY country, currency lets the database compute MIN/MAX/AVG/COUNT and return about 8 rows instead of 10,000 employees.
    // INTERVIEW: "Aggregates run in SQL, never in Java loops: the response size does not grow with the number of employees."
    @Query("""
            select new com.acme.salary.repository.SalaryStatsRow(
                e.country, e.currency, min(e.salary), max(e.salary), avg(e.salary), count(e))
            from Employee e
            group by e.country, e.currency
            order by e.country, e.currency
            """)
    List<SalaryStatsRow> salaryStatsByCountry();

    /**
     * Salary statistics per job title inside one country.
     *
     * @param country exact country name
     * @return one row per job title and currency, highest average first
     */
    // WHY: Same idea, but filtered by one country (WHERE uses the country index) and grouped by job title. ORDER BY avg desc shows the best-paid titles first; jobTitle breaks ties.
    // WHY: ":country" is a named parameter; the value is bound safely, so there is no SQL injection.
    @Query("""
            select new com.acme.salary.repository.SalaryStatsRow(
                e.jobTitle, e.currency, min(e.salary), max(e.salary), avg(e.salary), count(e))
            from Employee e
            where e.country = :country
            group by e.jobTitle, e.currency
            order by avg(e.salary) desc, e.jobTitle
            """)
    // WHY: @Param("country") links the Java parameter to ":country" in the query.
    List<SalaryStatsRow> salaryStatsByJobTitle(@Param("country") String country);

    /**
     * @return number of employees per country, largest first
     */
    // WHY: COUNT per country; no currency needed because a headcount is not money. Largest country first, name as tie-breaker.
    @Query("""
            select new com.acme.salary.repository.HeadcountRow(e.country, count(e))
            from Employee e
            group by e.country
            order by count(e) desc, e.country
            """)
    List<HeadcountRow> headcountByCountry();

    /**
     * @return every distinct country, alphabetically (used to fill the filter dropdowns)
     */
    // WHY: SELECT DISTINCT returns each value once; only a short list of strings is sent, not the employees.
    @Query("select distinct e.country from Employee e order by e.country")
    List<String> findDistinctCountries();

    /**
     * @return every distinct job title, alphabetically
     */
    // WHY: Values for the job title dropdown.
    @Query("select distinct e.jobTitle from Employee e order by e.jobTitle")
    List<String> findDistinctJobTitles();

    /**
     * @return every distinct department, alphabetically
     */
    // WHY: Values for the department dropdown.
    @Query("select distinct e.department from Employee e order by e.department")
    List<String> findDistinctDepartments();
}
