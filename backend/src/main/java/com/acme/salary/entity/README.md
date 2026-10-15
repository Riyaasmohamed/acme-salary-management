# `entity` — the database table as a Java class

## `Employee`

**What it does:** maps one row of the `employee` table to a Java object using JPA
annotations (`@Entity`, `@Column`, …).

**Responsibility:** hold the data of one employee and keep the `createdAt` / `updatedAt`
timestamps correct (`@PrePersist` sets both on insert, `@PreUpdate` refreshes `updatedAt`).

**Why it exists:** Hibernate needs a class to read and write rows. It contains no business
rules and is never sent to the browser — the API uses DTOs instead (see `dto/`).

Things worth knowing:

- **Ids come from a sequence** (`employee_seq`, `allocationSize = 50`). Hibernate reserves 50
  ids at a time, which lets it send inserts to the database in batches. With
  `GenerationType.IDENTITY` every insert would be a separate round trip.
- **`salary` is a `BigDecimal`** stored as `DECIMAL(14, 2)`, so money is exact.
- **The table itself is created by Flyway** (`src/main/resources/db/migration`), not by
  Hibernate. On start-up Hibernate only checks that this class matches the table
  (`spring.jpa.hibernate.ddl-auto=validate`).
- Getters and setters are written by hand (no Lombok) so everything is visible.
