// STUDY NOTES: Runs once after the app starts and fills an EMPTY employee table with demo employees, in one transaction. It never touches a table that already has data.
package com.acme.salary.seed;

import com.acme.salary.config.SeedProperties;
import com.acme.salary.entity.Employee;
import com.acme.salary.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Fills an empty database with demo employees when the application starts. It never touches
 * a database that already has employees, so restarting the app keeps the HR Manager's edits.
 * All rows are inserted in one transaction: either every employee is saved or none is, so a
 * failed seed simply runs again on the next start.
 */
// WHY: @Component makes it a Spring bean, so Spring creates it and injects its dependencies.
@Component
// WHY: ApplicationRunner means Spring calls run() once when start-up is finished (after Flyway has created the table).
public class EmployeeSeeder implements ApplicationRunner {

    // WHY: Logs what the seeder did, so you can see in the console whether it seeded or skipped.
    private static final Logger log = LoggerFactory.getLogger(EmployeeSeeder.class);

    // WHY: Constructor-injected, final dependencies: the repository to save and the typed seed settings.
    private final EmployeeRepository repository;
    private final SeedProperties properties;

    /**
     * @param repository employee data access
     * @param properties seed settings
     */
    // WHY: Constructor injection: easy to test with mocks (see EmployeeSeederTest).
    public EmployeeSeeder(EmployeeRepository repository, SeedProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Seeds the database if seeding is enabled and the employee table is empty.
     *
     * @param args application arguments (unused)
     */
    @Override
    // WHY: One transaction for all rows: either every employee is saved or none is, so the API never shows a half-seeded table and a failed seed retries on the next start.
    // INTERVIEW: "The first version saved in 1,000-row chunks and the API could briefly show a half-filled table; one transaction fixed that."
    @Transactional
    public void run(ApplicationArguments args) {
        // WHY: A switch in configuration; tests set app.seed.enabled=false so they start with an empty database.
        if (!properties.enabled()) {
            log.info("Seeding disabled (app.seed.enabled=false)");
            return;
        }
        // WHY: Only seed an empty table, so restarting the app never overwrites the HR Manager's changes.
        long existing = repository.count();
        if (existing > 0) {
            log.info("Skipping seed: database already has {} employees", existing);
            return;
        }
        // WHY: Measure how long the seed takes, to log it (about 2-3 seconds for 10,000 rows).
        long start = System.currentTimeMillis();
        // WHY: Build all employees in memory first, using the configured seed and count; today's date is the reference for hire dates.
        List<Employee> employees = new EmployeeDataGenerator(properties.randomSeed())
                .generate(properties.employeeCount(), LocalDate.now());
        // WHY: saveAllAndFlush sends all inserts now, in JDBC batches of 500 (batch_size) with ids from the sequence; flush makes any error happen inside this transaction.
        repository.saveAllAndFlush(employees);
        log.info("Seeded {} employees in {} ms", employees.size(), System.currentTimeMillis() - start);
    }
}
