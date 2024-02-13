package uk.gov.pay.api.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class ZoneDateTimeValidator implements ConstraintValidator<ValidZonedDateTime, String> {
    
    @Override
    public boolean isValid(String date, ConstraintValidatorContext context) {
        
        if (date == null) {
            return true;
        }
        
        try {
            ZonedDateTime.parse(date);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
