package uk.gov.pay.api.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class CardFirstSixDigitsValidator implements ConstraintValidator<ValidCardFirstSixDigits, String> {

    private Pattern pattern = Pattern.compile("\\d{6}");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        return pattern.matcher(value).matches();

    }
    
}
