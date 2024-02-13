package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class GetChargeException extends ConnectorResponseErrorException {

    public GetChargeException(Response response) {
        super(response);
    }
}
