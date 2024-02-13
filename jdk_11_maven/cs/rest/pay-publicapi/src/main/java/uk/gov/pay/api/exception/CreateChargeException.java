package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class CreateChargeException extends ConnectorResponseErrorException {

    public CreateChargeException(Response response) {
        super(response);
    }
}
