package uk.gov.pay.api.validation;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

class MaxLengthValidator {
    static boolean isInvalid(String value, int maxLength) {
        return isNotBlank(value) && value.length() > maxLength;
    }
}
