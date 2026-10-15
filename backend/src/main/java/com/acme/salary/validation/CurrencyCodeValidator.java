// STUDY NOTES: The logic behind @ValidCurrency. Bean Validation calls isValid() for every field marked with the annotation and checks it against the JDK's ISO 4217 currency list.
package com.acme.salary.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Locale;

/**
 * Checks a value against the ISO 4217 currency list that ships with the JDK.
 */
// WHY: ConstraintValidator<ValidCurrency, String> means "validates the @ValidCurrency annotation on String values".
public class CurrencyCodeValidator implements ConstraintValidator<ValidCurrency, String> {

    /**
     * @param value   the currency code to check, case-insensitive
     * @param context validation context (unused)
     * @return true for {@code null} or a known three-letter currency code
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // WHY: null is "valid" here on purpose: @NotBlank decides if the field is required, so each annotation checks only one thing (and the user gets one clear message).
        if (value == null) {
            return true;
        }
        // WHY: Case-insensitive: "usd" is accepted here; the mapper stores it as "USD".
        String code = value.trim().toUpperCase(Locale.ROOT);
        // WHY: Quick shape check first: exactly three letters A-Z.
        if (!code.matches("[A-Z]{3}")) {
            return false;
        }
        // WHY: Currency.getInstance throws for unknown codes, so "XYZ" fails even though it has the right shape.
        // INTERVIEW: "A regex like [A-Z]{3} would accept XYZ; the JDK's built-in currency list gives a real ISO 4217 check with no extra library."
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException unknownCode) {
            return false;
        }
    }
}
