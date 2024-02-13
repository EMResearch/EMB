package uk.gov.pay.api.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE, TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = PaymentOutcomeValidator.class)
@Documented
public @interface ValidPaymentOutcome {
    
    String message() default "Must include a valid status and error code";
    
    Class<?>[] groups() default{};
    
    Class<? extends Payload>[] payload() default{};
}
