package uk.gov.pay.api.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.apache.commons.lang3.StringUtils.isBlank;

class ExactLengthOrEmptyValidator implements ConstraintValidator<ExactLengthOrEmpty, String>  {

    private int length;

    static boolean isValid(String value, int length) {
        return isBlank(value) || value.length() == length;
    }

    @Override
    public void initialize(ExactLengthOrEmpty constraintAnnotation) {
        this.length = constraintAnnotation.length();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isValid(value, length);
    }
}
