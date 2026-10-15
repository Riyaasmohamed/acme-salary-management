# `validation` — custom validation rule

## `ValidCurrency` (annotation) and `CurrencyCodeValidator`

**What they do:** `@ValidCurrency` on a field means "this must be an ISO 4217 currency
code". `CurrencyCodeValidator` checks the value against the currency list built into the
JDK (`java.util.Currency`). It is case-insensitive (`usd` is accepted and later upper-cased
by the mapper). `null` is accepted on purpose — `@NotBlank` is what makes the field
required, so each annotation checks one thing.

**Why it exists:** a simple pattern like `[A-Z]{3}` would accept `XYZ`. Bean Validation has
no built-in currency rule, so this small custom constraint plugs into the same mechanism as
`@NotBlank` and `@Email`, and its message appears in the same `errors` map.
