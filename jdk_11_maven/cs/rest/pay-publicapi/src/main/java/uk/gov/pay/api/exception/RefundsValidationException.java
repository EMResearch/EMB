package uk.gov.pay.api.exception;

import uk.gov.pay.api.model.RequestError;

public class RefundsValidationException extends RuntimeException {

    private RequestError requestError;

    public RefundsValidationException(RequestError requestError) {
        this.requestError = requestError;
    }

    public RequestError getRequestError() {
        return requestError;
    }

    @Override
    public String toString() {
        return "RefundsValidationException{" +
                "refundError=" + requestError +
                '}';
    }
}
