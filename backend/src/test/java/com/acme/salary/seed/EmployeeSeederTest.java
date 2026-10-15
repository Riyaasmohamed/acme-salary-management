// STUDY NOTES: Checks EmployeeSeeder, the startup runner that fills an EMPTY database with demo data.
// STUDY NOTES: UNIT test with Mockito: the repository is a mock, so no database and no Spring are needed.
// STUDY NOTES: We only care about the seeder's DECISIONS (seed or skip), and a mock lets us control
// STUDY NOTES: count() and check whether saveAllAndFlush() was called.
package com.acme.salary.seed;

import com.acme.salary.config.SeedProperties;
import com.acme.salary.entity.Employee;
import com.acme.salary.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// WHY: ArgumentCaptor grabs the real argument passed to a mock so we can inspect it.
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
// WHY: The JUnit 5 extension that creates the @Mock fields before each test.
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// WHY: Turns on Mockito for this class. It also uses strict stubs: an unused when(...) fails the test,
// WHY: which keeps tests clean.
@ExtendWith(MockitoExtension.class)
class EmployeeSeederTest {

    // WHY: A fake repository. It returns defaults (0, null) unless we stub it, and it records every call.
    @Mock
    private EmployeeRepository repository;

    // WHY: @SuppressWarnings is needed because ArgumentCaptor.forClass(List.class) loses the generic type.
    @Test
    @SuppressWarnings("unchecked")
    void seedsAnEmptyDatabaseWithTheConfiguredNumberOfEmployees() {
        // WHY: Arrange: pretend the employee table is empty.
        when(repository.count()).thenReturn(0L);

        // WHY: Act: build the seeder by hand (constructor injection makes this easy) with seeding on
        // WHY: and 2,500 employees, then run it. ApplicationArguments are unused, so null is fine.
        new EmployeeSeeder(repository, new SeedProperties(true, 2_500, 42)).run(null);

        // WHY: Assert: capture the list given to saveAllAndFlush and check its size.
        // WHY: verify(...) also proves the save happened exactly once.
        ArgumentCaptor<List<Employee>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAllAndFlush(saved.capture());
        // WHY: Proves the configured count (not a hard-coded 10,000) is used.
        assertThat(saved.getValue()).hasSize(2_500);
    }

    // WHY: The most important safety rule: restarting the app must never add demo rows on top of
    // WHY: real data or the HR manager's edits.
    @Test
    void neverTouchesADatabaseThatAlreadyHasEmployees() {
        // WHY: Arrange: the table already has 10 rows.
        when(repository.count()).thenReturn(10L);

        new EmployeeSeeder(repository, new SeedProperties(true, 10_000, 42)).run(null);

        // WHY: never() + any() = "saveAllAndFlush was not called with ANY argument".
        verify(repository, never()).saveAllAndFlush(any());
    }

    // WHY: app.seed.enabled=false (used by our tests) must switch seeding off completely.
    @Test
    void doesNothingWhenDisabled() {
        new EmployeeSeeder(repository, new SeedProperties(false, 10_000, 42)).run(null);

        // WHY: Not even count() is called: the seeder returns before touching the database at all.
        verify(repository, never()).count();
        verify(repository, never()).saveAllAndFlush(any());
    }
}
