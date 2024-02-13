package uk.gov.pay.api.validation;

import uk.gov.pay.api.model.telephone.PaymentOutcome;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;

public class PaymentOutcomeValidator implements ConstraintValidator<ValidPaymentOutcome, PaymentOutcome> {

    private final static HashSet<String> ERROR_CODES = new HashSet<>();

    static {
        ERROR_CODES.add("P0010");
        ERROR_CODES.add("P0030");
        ERROR_CODES.add("P0050");
    }
    
    @Override
    public boolean isValid(PaymentOutcome paymentOutcome, ConstraintValidatorContext context) {
        
        if(paymentOutcome == null) {
            return true;
        }

        if("success".equals(paymentOutcome.getStatus()) && paymentOutcome.getCode().isEmpty()) {
            return true;
        }

        return "failed".equals(paymentOutcome.getStatus()) && (ERROR_CODES.contains(paymentOutcome.getCode().orElse(null)));
    }
}
