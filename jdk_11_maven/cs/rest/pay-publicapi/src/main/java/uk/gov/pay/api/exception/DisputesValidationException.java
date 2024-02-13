package uk.gov.pay.api.exception;

import uk.gov.pay.api.model.RequestError;

public class DisputesValidationException extends RuntimeException {

    private RequestError requestError;

    public DisputesValidationException(RequestError requestError) {
        this.requestError = requestError;
    }

    public RequestError getRequestError() {
        return requestError;
    }

    @Override
    public String toString() {
        return "DisputesValidationException{" +
                "disputeError=" + requestError +
                '}';
    }
}
