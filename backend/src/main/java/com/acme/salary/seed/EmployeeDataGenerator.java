// STUDY NOTES: Creates realistic, repeatable demo employees in memory (no database). The seeder saves what this class returns.
// WHY: Kept separate from EmployeeSeeder so it is a pure function of (seed, count, today) and easy to unit-test without Spring or a database.
package com.acme.salary.seed;

import com.acme.salary.entity.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Generates realistic, repeatable demo employees. The same seed always produces the same
 * employees, which keeps the demo and the tests deterministic.
 *
 * <p>Salary = base salary of the job (in USD terms) x country pay factor (converts to a
 * realistic local amount) x a random spread of 0.85-1.25, rounded to the nearest 100.
 */
// WHY: A plain class, not a Spring bean: it needs only a seed, so the seeder simply creates it with "new".
public class EmployeeDataGenerator {

    // WHY: Hire dates are spread over the last 15 years; a named constant explains the number.
    private static final int MAX_YEARS_OF_SERVICE = 15;

    // WHY: 8 countries, each with its currency, a pay factor (turns a USD-based salary into a realistic local amount) and a weight (its share of staff, in percent).
    // WHY: List.of creates an unmodifiable list, so the reference data cannot be changed by accident.
    private static final List<CountryProfile> COUNTRIES = List.of(
            new CountryProfile("United States", "USD", 1.00, 22),
            new CountryProfile("India", "INR", 18.00, 25),
            new CountryProfile("United Kingdom", "GBP", 0.62, 12),
            new CountryProfile("Germany", "EUR", 0.68, 10),
            new CountryProfile("Canada", "CAD", 1.05, 9),
            new CountryProfile("Australia", "AUD", 1.15, 8),
            new CountryProfile("Singapore", "SGD", 1.00, 7),
            new CountryProfile("Japan", "JPY", 85.00, 7));

    // WHY: 18 job titles in 10 departments, each with a base salary in USD terms.
    private static final List<JobProfile> JOBS = List.of(
            new JobProfile("Software Engineer", "Engineering", 110_000),
            new JobProfile("Senior Software Engineer", "Engineering", 145_000),
            new JobProfile("Engineering Manager", "Engineering", 175_000),
            new JobProfile("QA Engineer", "Engineering", 85_000),
            new JobProfile("DevOps Engineer", "Engineering", 120_000),
            new JobProfile("Product Manager", "Product", 140_000),
            new JobProfile("UX Designer", "Design", 95_000),
            new JobProfile("Data Analyst", "Data", 80_000),
            new JobProfile("Data Scientist", "Data", 130_000),
            new JobProfile("Sales Executive", "Sales", 70_000),
            new JobProfile("Account Manager", "Sales", 85_000),
            new JobProfile("Marketing Specialist", "Marketing", 70_000),
            new JobProfile("HR Specialist", "Human Resources", 65_000),
            new JobProfile("Recruiter", "Human Resources", 60_000),
            new JobProfile("Financial Analyst", "Finance", 80_000),
            new JobProfile("Accountant", "Finance", 65_000),
            new JobProfile("Customer Support Specialist", "Customer Support", 45_000),
            new JobProfile("Operations Manager", "Operations", 100_000));

    // WHY: A mix of names from the 8 countries so the demo data looks realistic.
    private static final List<String> FIRST_NAMES = List.of(
            "Aarav", "Aisha", "Akira", "Amelia", "Ananya", "Arjun", "Ben", "Carlos", "Chloe", "Daniel",
            "Elena", "Emma", "Ethan", "Fatima", "Hana", "Hiroshi", "Isabella", "James", "Jia", "Kavya",
            "Lena", "Liam", "Lucas", "Maya", "Meera", "Mohammed", "Noah", "Olivia", "Priya", "Rahul",
            "Ravi", "Sakura", "Sara", "Sofia", "Tom", "Wei", "William", "Yuki", "Zara", "Zoe",
            "Aditi", "Alex", "Anika", "Chen", "David", "Deepak", "Diego", "Emily", "Felix", "Grace",
            "Hannah", "Ishaan", "Jack", "Julia", "Kenji", "Leo", "Lily", "Mei", "Nikhil", "Nina",
            "Omar", "Oscar", "Paul", "Rohan", "Ryan", "Sanjay", "Sneha", "Sophie", "Vikram", "Yusuf");

    // WHY: 70 first names x 70 last names give many combinations; repeats get a number in the email.
    private static final List<String> LAST_NAMES = List.of(
            "Anderson", "Brown", "Chen", "Clarke", "Das", "Fischer", "Garcia", "Gupta", "Ito", "Iyer",
            "Johnson", "Khan", "Kim", "Kumar", "Lee", "Martin", "Meyer", "Miller", "Nair", "Nguyen",
            "O'Brien", "Patel", "Rao", "Reddy", "Rossi", "Sato", "Schmidt", "Shah", "Sharma", "Singh",
            "Smith", "Suzuki", "Tan", "Taylor", "Thomas", "Wagner", "Walker", "Wang", "Williams", "Wilson",
            "Ahmed", "Becker", "Bose", "Campbell", "Costa", "Davis", "Evans", "Fernandes", "Hall", "Hughes",
            "Jain", "Jones", "Kapoor", "Kaur", "Lim", "Lopez", "Menon", "Mishra", "Mori", "Murphy",
            "Ng", "Pillai", "Roberts", "Scott", "Takahashi", "Verma", "Weber", "White", "Wong", "Young");

    // WHY: The random seed; the same seed always produces the same employees.
    private final long seed;

    /**
     * @param seed random seed; the same seed always yields the same employees
     */
    public EmployeeDataGenerator(long seed) {
        this.seed = seed;
    }

    /**
     * @param count number of employees to create
     * @param today reference date; hire dates are never after it
     * @return new, unsaved employees with unique emails
     */
    // WHY: "today" is a parameter (not LocalDate.now() inside) so tests can pass a fixed date and get fixed results.
    public List<Employee> generate(int count, LocalDate today) {
        // WHY: java.util.Random with a fixed seed is deterministic: the same sequence of numbers every run.
        Random random = new Random(seed);
        // WHY: Counts how often each "first.last" was used, to make every email unique.
        Map<String, Integer> emailUses = new HashMap<>();
        // WHY: Initial capacity = count, so the list does not need to grow while adding 10,000 items.
        List<Employee> employees = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            employees.add(createEmployee(random, emailUses, today));
        }
        return employees;
    }

    // WHY: Builds one employee from random picks. The order of random calls matters, because changing it would change the generated data.
    private Employee createEmployee(Random random, Map<String, Integer> emailUses, LocalDate today) {
        String firstName = pick(FIRST_NAMES, random);
        String lastName = pick(LAST_NAMES, random);
        // WHY: Weighted pick, so India gets about 25% of staff and Japan about 7%, like a real company.
        CountryProfile country = pickWeighted(random);
        JobProfile job = pick(JOBS, random);

        // WHY: A new entity without id; Hibernate assigns ids from the sequence when it is saved.
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(uniqueEmail(firstName, lastName, emailUses));
        employee.setJobTitle(job.title());
        employee.setDepartment(job.department());
        employee.setCountry(country.name());
        // WHY: The currency always comes from the country, so each country has exactly one currency.
        employee.setCurrency(country.currency());
        employee.setSalary(salary(job, country, random));
        // WHY: 0 to 15*365 days back from today, so a hire date is never in the future (+1 because nextInt's upper bound is exclusive).
        employee.setHireDate(today.minusDays(random.nextInt(MAX_YEARS_OF_SERVICE * 365 + 1)));
        return employee;
    }

    // WHY: salary = base x country pay factor x random spread, so people with the same job are not all paid the same.
    private static BigDecimal salary(JobProfile job, CountryProfile country, Random random) {
        // WHY: nextDouble() is 0.0 to 1.0, so the spread is 0.85 to 1.25 (-15% to +25%).
        double spread = 0.85 + random.nextDouble() * 0.40;
        // WHY: Divide by 100, round, multiply by 100 = round to the nearest 100, like real salary offers. double is fine here because the result is rounded before it becomes money.
        long roundedToHundred = Math.round(job.baseSalary() * country.payFactor() * spread / 100.0) * 100;
        // WHY: Convert to BigDecimal with 2 decimals to match DECIMAL(14,2) in the table.
        return BigDecimal.valueOf(roundedToHundred).setScale(2);
    }

    /**
     * First "asha.rao@acme.com", then "asha.rao2@acme.com", "asha.rao3@acme.com", ...
     */
    // WHY: The email column has a unique index, so the generator must never produce the same email twice.
    private static String uniqueEmail(String firstName, String lastName, Map<String, Integer> emailUses) {
        // WHY: Lower-case like the mapper does; remove the apostrophe in names such as O'Brien to keep the email simple.
        String local = (firstName + "." + lastName).toLowerCase(Locale.ROOT).replace("'", "");
        // WHY: merge adds 1 to the counter (or starts it at 1) and returns the new count in one call.
        int uses = emailUses.merge(local, 1, Integer::sum);
        // WHY: First use has no number, later uses get 2, 3, ... (asha.rao2@acme.com).
        return (uses == 1 ? local : local + uses) + "@acme.com";
    }

    // WHY: Generic helper: one random element from any list.
    private static <T> T pick(List<T> values, Random random) {
        return values.get(random.nextInt(values.size()));
    }

    // WHY: Weighted random choice: draw a "ticket" from 0 to the total weight, then walk the list subtracting weights until the ticket is used up.
    private static CountryProfile pickWeighted(Random random) {
        int totalWeight = COUNTRIES.stream().mapToInt(CountryProfile::weight).sum();
        int ticket = random.nextInt(totalWeight);
        for (CountryProfile country : COUNTRIES) {
            ticket -= country.weight();
            if (ticket < 0) {
                return country;
            }
        }
        // WHY: Should never happen; the compiler needs a return or throw after the loop, and this fails loudly if the logic is broken.
        throw new IllegalStateException("Weights do not add up");
    }

    /** A country, its currency, how pay compares to the USD base, and its share of staff. */
    // WHY: Private records: small, immutable data holders used only inside this class.
    private record CountryProfile(String name, String currency, double payFactor, int weight) {
    }

    /** A job title, the department it belongs to and its base salary in USD terms. */
    private record JobProfile(String title, String department, int baseSalary) {
    }
}
