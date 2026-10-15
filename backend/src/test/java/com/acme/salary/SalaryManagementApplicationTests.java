// STUDY NOTES: Smoke test that starts the WHOLE Spring application once (all beans, Flyway migration,
// STUDY NOTES: Hibernate schema validation). It is a full integration test, not a unit test. We keep only
// STUDY NOTES: ONE such test because it is the slowest kind; the detailed checks live in fast unit and slice tests.
package com.acme.salary;

// WHY: JUnit 5 annotation that marks a method as a test.
import org.junit.jupiter.api.Test;
// WHY: Annotation that boots the full Spring context for the test.
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the whole application context (all beans, Flyway, JPA validation) starts.
 */
// WHY: @SpringBootTest finds SalaryManagementApplication and starts everything, like a real run.
// WHY: It uses src/test/resources/application.properties, so it runs on in-memory H2 with no seed data.
// INTERVIEW: This catches wiring mistakes (missing bean, bad property, entity not matching the SQL table)
// INTERVIEW: that unit tests with mocks can never see.
@SpringBootTest
class SalaryManagementApplicationTests {

    // WHY: The test body is empty on purpose. If the context fails to start, Spring throws
    // WHY: before this method runs and the test fails. Passing = "the app can start".
    @Test
    void contextLoads() {
    }
}
