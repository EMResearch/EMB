package uk.gov.pay.api.exception;

import uk.gov.pay.api.model.RequestError;

public class AgreementValidationException extends RuntimeException {

    private RequestError requestError;

    public AgreementValidationException(RequestError requestError) {
        this.requestError = requestError;
    }

    public RequestError getRequestError() {
        return requestError;
    }

    @Override
    public String toString() {
        return "AgreementValidationException{" +
                "requestError=" + requestError +
                '}';
    }

}
