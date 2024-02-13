package uk.gov.pay.api.validation;

import java.util.regex.Pattern;

import static org.eclipse.jetty.util.StringUtil.isBlank;

class NumericValidator {
    private static final Pattern ALL_DIGITS = Pattern.compile("[0-9]+");

    static boolean isValidOrNull(String value) {
        return isBlank(value) || ALL_DIGITS.matcher(value).matches();
    }

    static boolean isValid(String value) {
        return ALL_DIGITS.matcher(value).matches();
    }
}
