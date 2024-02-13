package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class CancelAgreementException extends ConnectorResponseErrorException {

    public CancelAgreementException(Response response) {
        super(response);
    }

}
