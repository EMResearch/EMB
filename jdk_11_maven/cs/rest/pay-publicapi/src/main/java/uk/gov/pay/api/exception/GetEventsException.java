package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class GetEventsException extends ConnectorResponseErrorException {

    public GetEventsException(Response response) {
        super(response);
    }
}
