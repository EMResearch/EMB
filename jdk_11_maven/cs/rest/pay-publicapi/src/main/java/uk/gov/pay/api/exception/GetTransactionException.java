package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class GetTransactionException extends ConnectorResponseErrorException {

    public GetTransactionException(Response response) {
        super(response);
    }
}
