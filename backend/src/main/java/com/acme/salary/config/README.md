# `config` — application settings

## `SeedProperties`

**What it does:** a record bound to the `app.seed.*` properties in
`src/main/resources/application.properties` (enabled through `@ConfigurationPropertiesScan`
on the application class):

```properties
app.seed.enabled=true          # seed on start-up (only if the table is empty)
app.seed.employee-count=10000  # how many employees to create
app.seed.random-seed=42        # same seed → same employees every time
```

**Why it exists:** settings live in configuration, not in code, so the seed can be switched
off (tests do this in `src/test/resources/application.properties`) or resized without
recompiling. Using a typed record instead of `@Value` strings means a typo in a property type
fails at start-up.
