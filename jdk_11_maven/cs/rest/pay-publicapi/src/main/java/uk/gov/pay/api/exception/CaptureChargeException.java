package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class CaptureChargeException extends ConnectorResponseErrorException {

    public CaptureChargeException(Response response) {
        super(response);
    }

}
