// STUDY NOTES: Checks CurrencyCodeValidator, the custom Bean Validation rule behind @ValidCurrency.
// STUDY NOTES: Plain UNIT test: we create the validator with "new" and call isValid() directly. No Spring,
// STUDY NOTES: no mocks, runs in milliseconds. That is enough because the class has no dependencies.
package com.acme.salary.validation;

// WHY: @ParameterizedTest runs the same test method once per input value.
import org.junit.jupiter.params.ParameterizedTest;
// WHY: @NullSource feeds a single null value to a parameterized test.
import org.junit.jupiter.params.provider.NullSource;
// WHY: @ValueSource feeds a list of literal values (here Strings) to a parameterized test.
import org.junit.jupiter.params.provider.ValueSource;

// WHY: AssertJ gives readable, fluent assertions: assertThat(x).isTrue().
import static org.assertj.core.api.Assertions.assertThat;

class CurrencyCodeValidatorTest {

    // WHY: The object under test. It has no state, so one instance per test class is safe.
    private final CurrencyCodeValidator validator = new CurrencyCodeValidator();

    // WHY: Happy path. Real ISO 4217 codes must pass. "jpy" proves case does not matter
    // WHY: (the validator upper-cases) and " sgd " proves spaces around the code are trimmed.
    // INTERVIEW: @ParameterizedTest gives one test report line per value, so you see exactly which input failed.
    @ParameterizedTest
    @ValueSource(strings = {"USD", "INR", "EUR", "GBP", "jpy", " sgd "})
    void acceptsKnownIsoCodesInAnyCase(String code) {
        // WHY: Act + Assert in one line. The context argument is unused by the validator, so null is fine.
        assertThat(validator.isValid(code, null)).isTrue();
    }

    // WHY: Sad path. Each value hits a different branch: "XYZ" has the right shape but is not a real
    // WHY: currency (Currency.getInstance throws); "US" is too short; "DOLLAR" too long; "" empty;
    // WHY: "12A" has digits. All must fail the [A-Z]{3} check or the JDK lookup.
    @ParameterizedTest
    @ValueSource(strings = {"XYZ", "US", "DOLLAR", "", "12A"})
    void rejectsUnknownOrMalformedCodes(String code) {
        assertThat(validator.isValid(code, null)).isFalse();
    }

    // WHY: null returns true on purpose. "Is it present?" is the job of @NotBlank on the same field.
    // WHY: Each validator checks one thing, so the user does not get two error messages for one empty field.
    @ParameterizedTest
    @NullSource
    void leavesNullToNotBlank(String code) {
        assertThat(validator.isValid(code, null)).isTrue();
    }
}
