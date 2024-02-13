package uk.gov.pay.api.exception.mapper;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_AGREEMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class CreateAgreementExceptionMapper implements ExceptionMapper<CreateAgreementException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAgreementExceptionMapper.class);
   
    @Override
    public Response toResponse(CreateAgreementException exception) {
        RequestError requestError;
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR_500;
        
        if (exception.getErrorIdentifier() == ErrorIdentifier.RECURRING_CARD_PAYMENTS_NOT_ALLOWED) {
            statusCode = HttpStatus.UNPROCESSABLE_ENTITY_422;
            requestError = aRequestError(RequestError.Code.RECURRING_CARD_PAYMENTS_NOT_ALLOWED_ERROR);
        }
        else {
            requestError = aRequestError(CREATE_AGREEMENT_CONNECTOR_ERROR);
            LOGGER.info("Connector invalid response was {}.\n Returning http status {} with error body {}",
                    exception.getMessage(), INTERNAL_SERVER_ERROR, requestError);
        }
        
        return Response.status(statusCode).entity(requestError).build();
    }
}
