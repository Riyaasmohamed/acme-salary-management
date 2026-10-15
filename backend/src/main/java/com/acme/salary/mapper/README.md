# `mapper` — converting between DTOs and the entity

## `EmployeeMapper`

**What it does:** three static methods: `toEntity(request)`, `copyInto(request, entity)`
and `toResponse(entity)`, plus `normaliseEmail(email)`.

**Responsibility:** copy fields between the API objects and the JPA entity, and **normalise
input in one place**:

- text fields are trimmed,
- email is lower-cased (so `Asha.Rao@ACME.com` and `asha.rao@acme.com` are the same person),
- currency is upper-cased (`inr` → `INR`).

**Why it exists:** the service should not be full of field-by-field copying, and the same
normalisation must apply to create and update. It is hand-written (no MapStruct) because it
is short and easy to read. It has no state, so static methods are enough.
