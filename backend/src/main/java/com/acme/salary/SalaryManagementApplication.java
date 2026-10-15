// STUDY NOTES: The main class that starts the whole backend. Running main() boots Spring, the embedded web server, the database and all our beans.
// WHY: This class sits in the root package com.acme.salary, so Spring's component scan finds every class in the sub-packages below it.
package com.acme.salary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the salary management API.
 */
// WHY: @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan. One annotation turns on auto-setup (web, JPA, Flyway) and finds our @Service/@RestController classes.
@SpringBootApplication
// WHY: Finds records with @ConfigurationProperties (our SeedProperties) and fills them from application.properties, without registering each one by hand.
@ConfigurationPropertiesScan
public class SalaryManagementApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    // WHY: A normal Java main method, so the app runs as a plain jar ("java -jar") with no external server to install.
    public static void main(String[] args) {
        // WHY: SpringApplication.run creates the application context, starts Tomcat on port 8080 and runs Flyway and the seeder.
        SpringApplication.run(SalaryManagementApplication.class, args);
    }
}
