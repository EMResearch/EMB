package uk.gov.pay.api.exception;
import javax.ws.rs.core.Response;

public class CreateAgreementException extends ConnectorResponseErrorException {
    public CreateAgreementException(Response response) {
        super(response);
    }
}
