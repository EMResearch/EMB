package uk.gov.pay.api.validation;

import uk.gov.service.payments.commons.model.CardExpiryDate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CardExpiryValidator implements ConstraintValidator<ValidCardExpiryDate, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || CardExpiryDate.CARD_EXPIRY_DATE_PATTERN.matcher(value).matches();
    }

}
