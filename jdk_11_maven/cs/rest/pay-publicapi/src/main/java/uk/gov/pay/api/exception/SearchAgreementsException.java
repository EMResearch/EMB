package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class SearchAgreementsException extends ConnectorResponseErrorException {

    public SearchAgreementsException(Response response) {
        super(response);
    }

    public SearchAgreementsException(Throwable cause) {
        super(cause);
    }
}
