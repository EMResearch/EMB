package uk.gov.pay.api.exception;

import javax.ws.rs.core.Response;

public class AuthorisationRequestException extends ConnectorResponseErrorException{
    private Response response;

    public AuthorisationRequestException(Response response) {
        super(response);
        this.response = response;
    }
}
