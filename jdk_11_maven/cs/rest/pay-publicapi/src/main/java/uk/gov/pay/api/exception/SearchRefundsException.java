package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class SearchRefundsException extends ConnectorResponseErrorException {

    public SearchRefundsException(Response response) {
        super(response);
    }

    public SearchRefundsException(Throwable cause) {
        super(cause);
    }
}
