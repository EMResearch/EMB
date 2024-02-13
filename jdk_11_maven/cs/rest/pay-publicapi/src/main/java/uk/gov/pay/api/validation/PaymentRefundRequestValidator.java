package uk.gov.pay.api.validation;

import uk.gov.pay.api.exception.PaymentValidationException;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.model.RequestError;

import static java.lang.String.format;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.AMOUNT_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.AMOUNT_MAX_VALUE;
import static uk.gov.pay.api.model.CreatePaymentRefundRequest.REFUND_MIN_VALUE;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class PaymentRefundRequestValidator {

    public void validate(CreatePaymentRefundRequest paymentRefundRequest) {
        validateAmount(paymentRefundRequest.getAmount());
    }

    private void validateAmount(int amount) {
        validate(amount >= REFUND_MIN_VALUE,
                aRequestError(AMOUNT_FIELD_NAME, CREATE_PAYMENT_REFUND_VALIDATION_ERROR, format("Must be greater than or equal to %d", REFUND_MIN_VALUE)));

        validate(amount <= AMOUNT_MAX_VALUE,
                aRequestError(AMOUNT_FIELD_NAME, CREATE_PAYMENT_REFUND_VALIDATION_ERROR, format("Must be less than or equal to %d", AMOUNT_MAX_VALUE)));
    }

    private static void validate(boolean condition, RequestError error) {
        if (!condition) {
            throw new PaymentValidationException(error);
        }
    }
}
