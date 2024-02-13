package uk.gov.pay.api.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ZoneDateTimeValidator.class)
@Documented
public @interface ValidZonedDateTime {

    String message() default "must be a valid ISO-8601 time and date format";

    Class<?>[] groups() default{};

    Class<? extends Payload>[] payload() default{};
    
}
