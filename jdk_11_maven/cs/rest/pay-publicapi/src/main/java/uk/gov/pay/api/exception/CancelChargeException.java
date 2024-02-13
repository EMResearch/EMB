package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class CancelChargeException extends ConnectorResponseErrorException {

    public CancelChargeException(Response response) {
        super(response);
    }
}
