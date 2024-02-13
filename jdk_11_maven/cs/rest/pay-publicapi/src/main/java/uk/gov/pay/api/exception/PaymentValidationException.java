package uk.gov.pay.api.exception;

import uk.gov.pay.api.model.RequestError;

public class PaymentValidationException extends RuntimeException {

    private RequestError requestError;

    public PaymentValidationException(RequestError requestError) {
        this.requestError = requestError;
    }

    public RequestError getRequestError() {
        return requestError;
    }

    @Override
    public String toString() {
        return "PaymentValidationException{" +
                "requestError=" + requestError +
                '}';
    }
}
