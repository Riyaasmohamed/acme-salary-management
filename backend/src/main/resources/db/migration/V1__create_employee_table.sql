-- STUDY NOTES: The first Flyway migration. It creates the employee table, its id sequence and its indexes; Flyway runs it once and records that in its history table.
-- WHY: The file name matters: "V1" is the version and "__" separates it from the description, so Flyway knows the order to run files.
-- Ids come from a sequence (not IDENTITY) so Hibernate can batch inserts.
-- INCREMENT BY 50 matches allocationSize = 50 on the entity.
-- WHY: Hibernate takes one sequence value and then uses the next 50 ids in memory, so 10,000 inserts need only about 200 sequence calls.
-- INTERVIEW: "With IDENTITY, Hibernate must insert rows one by one to learn each id, which turns off JDBC batching."
CREATE SEQUENCE employee_seq START WITH 1 INCREMENT BY 50;

-- WHY: One table is enough: the scope is one entity with CRUD and reports.
CREATE TABLE employee (
    -- WHY: BIGINT primary key; the value comes from employee_seq through Hibernate, not from the database.
    id          BIGINT         NOT NULL PRIMARY KEY,
    -- WHY: NOT NULL on every column repeats the @NotBlank rules, so bad data cannot get in even without the API.
    first_name  VARCHAR(100)   NOT NULL,
    last_name   VARCHAR(100)   NOT NULL,
    -- WHY: 254 is the longest valid email address, so the column fits any real email.
    email       VARCHAR(254)   NOT NULL,
    job_title   VARCHAR(100)   NOT NULL,
    department  VARCHAR(100)   NOT NULL,
    country     VARCHAR(100)   NOT NULL,
    -- WHY: DECIMAL(14, 2) stores money exactly with 2 decimal places (up to 12 digits before the point); a FLOAT would round.
    salary      DECIMAL(14, 2) NOT NULL,
    -- WHY: An ISO 4217 currency code is always 3 letters, like INR or USD.
    currency    VARCHAR(3)     NOT NULL,
    -- WHY: DATE has no time part; a hire date is a calendar day, which matches LocalDate in Java.
    hire_date   DATE           NOT NULL,
    -- WHY: TIMESTAMP WITH TIME ZONE matches the Instant fields; the entity sets these values, not the database.
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    -- WHY: The unique constraint is the real guarantee of unique emails, even if two requests pass the service check at the same time.
    CONSTRAINT uk_employee_email UNIQUE (email),
    -- WHY: The CHECK rejects zero or negative salaries at the database level too, as a last safety net behind @Positive.
    CONSTRAINT ck_employee_salary_positive CHECK (salary > 0)
);

-- Columns used by the list filters and by the GROUP BY of the insight queries.
-- WHY: Without an index, "WHERE country = 'India'" scans all 10,000 rows; with it, only the matching rows are read (16x fewer page reads measured).
CREATE INDEX idx_employee_country ON employee (country);
-- WHY: Speeds up the job title filter on the employee list.
CREATE INDEX idx_employee_job_title ON employee (job_title);
-- WHY: Speeds up the department filter on the employee list.
CREATE INDEX idx_employee_department ON employee (department);
