// STUDY NOTES: The JPA entity: one Employee object = one row of the employee table. Hibernate uses it to read and write rows; it is never sent to the browser (DTOs are).
package com.acme.salary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One employee and their current annual salary, mapped to the {@code employee} table.
 */
// WHY: @Entity tells Hibernate this class is stored in the database and can be loaded, saved and queried.
@Entity
// WHY: @Table names the real table created by the Flyway migration; with ddl-auto=validate Hibernate checks they match at start-up.
@Table(name = "employee")
// WHY: A normal class (not a record) because JPA needs a no-argument constructor and setters to fill and change the object.
public class Employee {

    // WHY: @Id marks the primary key.
    @Id
    // WHY: The id comes from a database sequence. SEQUENCE (not IDENTITY) lets Hibernate know ids before inserting, so it can batch inserts.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    // WHY: allocationSize = 50 must equal "INCREMENT BY 50" in the migration; Hibernate reserves 50 ids per database call.
    // INTERVIEW: "Sequence ids with allocationSize 50 plus batch_size 500 make the 10,000-row seed take 2-3 seconds; IDENTITY would disable batching."
    @SequenceGenerator(name = "employee_seq", sequenceName = "employee_seq", allocationSize = 50)
    // WHY: Long (object, not long) so it can be null before the first save, which means "new row".
    private Long id;

    // WHY: @Column gives the exact column name (snake_case in SQL), NOT NULL and the length, matching the migration.
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    // WHY: No name needed because the field and column are both "email". Stored lower-case by the mapper; the unique index lives in the migration.
    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "job_title", nullable = false, length = 100)
    private String jobTitle;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    private String country;

    // WHY: precision 14, scale 2 = DECIMAL(14,2). BigDecimal, not double, because double cannot store 0.10 exactly and money must be exact.
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal salary;

    // WHY: 3 characters for an ISO 4217 code like "INR".
    @Column(nullable = false, length = 3)
    private String currency;

    // WHY: LocalDate = a calendar day with no time or time zone, which is what a hire date is.
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    // WHY: updatable = false means Hibernate never changes created_at after the insert.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // WHY: Instant is an exact moment in UTC, good for audit timestamps.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Sets both timestamps when the row is first inserted.
     */
    // WHY: @PrePersist is a JPA callback: Hibernate calls this method just before the INSERT, so no service can forget to set the timestamps.
    @PrePersist
    void onCreate() {
        // WHY: One "now" for both fields, so createdAt and updatedAt are exactly equal on a new row.
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Refreshes the last-modified timestamp on every update.
     */
    // WHY: @PreUpdate runs just before every UPDATE, so updatedAt is always fresh.
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // WHY: Getters and setters are written by hand (no Lombok) so everything is visible in a code review. Hibernate and the mapper use them.
    /** @return database id, or {@code null} before the first save */
    public Long getId() {
        return id;
    }

    /** @param id database id (used by tests and by the update flow) */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return first name */
    public String getFirstName() {
        return firstName;
    }

    /** @param firstName first name */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** @return last name */
    public String getLastName() {
        return lastName;
    }

    /** @param lastName last name */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /** @return work email, stored in lower case */
    public String getEmail() {
        return email;
    }

    /** @param email work email */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return job title, e.g. "Software Engineer" */
    public String getJobTitle() {
        return jobTitle;
    }

    /** @param jobTitle job title */
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    /** @return department, e.g. "Engineering" */
    public String getDepartment() {
        return department;
    }

    /** @param department department */
    public void setDepartment(String department) {
        this.department = department;
    }

    /** @return country of employment */
    public String getCountry() {
        return country;
    }

    /** @param country country of employment */
    public void setCountry(String country) {
        this.country = country;
    }

    /** @return annual salary in {@link #getCurrency()} */
    public BigDecimal getSalary() {
        return salary;
    }

    /** @param salary annual salary */
    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    /** @return ISO 4217 currency code, e.g. "INR" */
    public String getCurrency() {
        return currency;
    }

    /** @param currency ISO 4217 currency code */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /** @return date the employee joined */
    public LocalDate getHireDate() {
        return hireDate;
    }

    /** @param hireDate date the employee joined */
    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    // WHY: Timestamps have getters only, no setters: only the @PrePersist/@PreUpdate callbacks may change them.
    /** @return when the row was created */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return when the row was last changed */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
