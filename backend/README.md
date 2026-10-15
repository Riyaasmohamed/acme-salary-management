# Backend — Spring Boot API

Java 17 · Spring Boot 3.5 · Spring Data JPA · Bean Validation · Flyway · H2 (file mode)

## Run

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The API listens on <http://localhost:8080/api>. On the first start the log shows
`Seeded 10000 employees in … ms`; later starts show `Skipping seed: database already has … employees`.
Data is stored in `backend/data/` (git-ignored). Delete that folder to start from fresh demo data.

## Test

```bash
./mvnw test
```

58 tests, about 20–30 seconds including Maven start-up. Tests use an in-memory database and never touch `backend/data/`.

## Package map

```
com.acme.salary
├── SalaryManagementApplication   entry point
├── controller   HTTP endpoints                     → controller/README.md
├── service      business rules, transactions       → service/README.md
├── repository   database access, SQL aggregates    → repository/README.md
├── entity       the employee table mapping         → entity/README.md
├── dto          JSON request/response records      → dto/README.md
├── mapper       DTO ⇄ entity, input normalisation  → mapper/README.md
├── exception    domain errors → HTTP errors        → exception/README.md
├── validation   @ValidCurrency rule                → validation/README.md
├── seed         10,000 demo employees              → seed/README.md
└── config       typed settings (app.seed.*)        → config/README.md
```

The schema is in `src/main/resources/db/migration/V1__create_employee_table.sql`.
