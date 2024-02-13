package uk.gov.pay.api.validation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.model.telephone.PaymentOutcome;
import uk.gov.pay.api.model.telephone.Supplemental;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PaymentOutcomeValidatorTest {

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
                .withCreatedDate("2018-02-21T16:04:25Z")
                .withAuthorisedDate("2018-02-21T16:05:33Z")
                .withAuthCode("666")
                .withProcessorId("1PROC")
                .withProviderId("1PROV")
                .withLastFourDigits("1234")
                .withFirstSixDigits("123456")
                .withCardExpiry("01/13")
                .withCardType("visa")
                .withNameOnCard("Jane Doe")
                .withEmailAddress("jane_doe@example.com")
                .withTelephoneNumber("+447700900796");
    }

    @Test
    public void failsValidationForInvalidPaymentOutcomeStatus() {

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withPaymentOutcome(new PaymentOutcome("invalid"))
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [payment_outcome] must include a valid status and error code"));
    }

    @Test
    public void failsValidationForPaymentOutcomeStatusSuccessAndErrorCodeGiven() {

        PaymentOutcome paymentOutcome = new PaymentOutcome(
                "success",
                "error",
                new Supplemental(
                        "ECKOH01234",
                        "textual message describing error code"
                )
        );

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withPaymentOutcome(paymentOutcome)
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [payment_outcome] must include a valid status and error code"));
    }

    @Test
    public void failsValidationForInvalidErrorCode() {

        PaymentOutcome paymentOutcome = new PaymentOutcome(
                "failed",
                "error",
                new Supplemental(
                        "ECKOH01234",
                        "textual message describing error code"
                )
        );

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withPaymentOutcome(paymentOutcome)
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [payment_outcome] must include a valid status and error code"));
    }

    @Test
    public void passesValidationForCorrectErrorCode() {

        PaymentOutcome paymentOutcome = new PaymentOutcome(
                "failed",
                "P0010",
                new Supplemental(
                        "ECKOH01234",
                        "textual message describing error code"
                )
        );

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withPaymentOutcome(paymentOutcome)
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.isEmpty(), is(true));
    }

    @Test
    public void passesValidationForPaymentOutcomeStatusOfSuccess() {

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withPaymentOutcome(new PaymentOutcome("success"))
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.isEmpty(), is(true));
    }

    @Test
    public void passesValidationForNullPaymentOutcome() {

        CreateTelephonePaymentRequest telephonePaymentRequest = builder
                .withPaymentOutcome(null)
                .build();

        Set<ConstraintViolation<CreateTelephonePaymentRequest>> constraintViolations = validator.validate(telephonePaymentRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [payment_outcome] cannot be null"));
    }
}
