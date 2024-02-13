package uk.gov.pay.api.exception;

import uk.gov.pay.api.model.RequestError;

public class BadRefundsRequestException extends RuntimeException {

    private RequestError refundError;

    public BadRefundsRequestException(RequestError refundError) {
        this.refundError = refundError;
    }
    
    public RequestError getRequestError() {
        return refundError;
    }

    @Override
    public String toString() {
        return "BadRefundsRequestException{" +
                "refundError=" + refundError +
                '}';
    }
}
