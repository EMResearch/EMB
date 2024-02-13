package uk.gov.pay.api.exception;

import uk.gov.pay.api.model.RequestError;

public class BadRequestException extends RuntimeException {

    private RequestError requestError;

    public BadRequestException(RequestError requestError) {
        this.requestError = requestError;
    }

    public RequestError getRequestError() {
        return requestError;
    }

    @Override
    public String toString() {
        return "BadRequestException{" +
                "requestError=" + requestError +
                '}';
    }
}
