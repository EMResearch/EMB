package uk.gov.pay.api.validation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.model.telephone.PaymentOutcome;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CardFirstSixDigitsValidatorTest {

    private static CreateTelephonePaymentRequest.Builder builder = new CreateTelephonePaymentRequest.Builder();

    private static Validator validator;

    @BeforeAll
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        builder
                .withAmount(1200)
                .withDescription("Some description")
                .withReference("Some reference")
                .withProcessorId("1PROC")
                .withProviderId("1PROV")
                .withCardExpiry("01/99")
                .withCardType("visa")
                .withLastFourDigits("1234")
                .withPaymentOutcome(new PaymentOutcome("success"));
    }
    
    @Test
    public void failsValidationForFiveDigits() {

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withFirstSixDigits("12345")
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [first_six_digits] must be exactly 6 digits"));
    }

    @Test
    public void failsValidationForSevenDigits() {

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withFirstSixDigits("1234567")
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [first_six_digits] must be exactly 6 digits"));
    }

    @Test
    public void passesValidationForSixDigits() {

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withFirstSixDigits("123456")
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.isEmpty(), is(true));
    }

    @Test
    public void passesValidationForNullDigits() {

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withFirstSixDigits(null)
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.isEmpty(), is(true));
    }
}
