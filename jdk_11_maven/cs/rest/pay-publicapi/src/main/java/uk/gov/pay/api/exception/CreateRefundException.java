package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class CreateRefundException extends ConnectorResponseErrorException {

    public CreateRefundException(Response response) {
        super(response);
    }
}
