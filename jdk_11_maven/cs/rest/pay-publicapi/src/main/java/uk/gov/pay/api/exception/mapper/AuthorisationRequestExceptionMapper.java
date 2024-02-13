package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.AuthorisationRequestException;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.PAYMENT_REQUIRED;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static uk.gov.pay.api.model.RequestError.Code.AUTHORISATION_CARD_NUMBER_REJECTED_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.AUTHORISATION_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.AUTHORISATION_ONE_TIME_TOKEN_ALREADY_USED_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.AUTHORISATION_ONE_TIME_TOKEN_INVALID_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.AUTHORISATION_REJECTED_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.GENERIC_VALIDATION_EXCEPTION_MESSAGE_FROM_CONNECTOR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class AuthorisationRequestExceptionMapper implements ExceptionMapper<AuthorisationRequestException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorisationRequestExceptionMapper.class);
    @Override
    public Response toResponse(AuthorisationRequestException exception) {
        int errorStatus;
        ErrorIdentifier errorIdentifier = exception.getErrorIdentifier();
        RequestError requestError;
        switch (errorIdentifier) {
            case CARD_NUMBER_REJECTED:
                errorStatus = PAYMENT_REQUIRED.getStatusCode();
                requestError = aRequestError(AUTHORISATION_CARD_NUMBER_REJECTED_ERROR, exception.getConnectorErrorMessage());
                break;
            case AUTHORISATION_REJECTED:
                errorStatus = PAYMENT_REQUIRED.getStatusCode();
                requestError = aRequestError(AUTHORISATION_REJECTED_ERROR, exception.getConnectorErrorMessage());
                break;
            case AUTHORISATION_ERROR:
            case AUTHORISATION_TIMEOUT:
                errorStatus = INTERNAL_SERVER_ERROR.getStatusCode();
                requestError = aRequestError(AUTHORISATION_ERROR, "There was an error authorising the payment");
                break;
            case ONE_TIME_TOKEN_ALREADY_USED:
                errorStatus = BAD_REQUEST.getStatusCode();
                requestError = aRequestError(AUTHORISATION_ONE_TIME_TOKEN_ALREADY_USED_ERROR, exception.getConnectorErrorMessage());
                break;
            case ONE_TIME_TOKEN_INVALID:
                errorStatus = BAD_REQUEST.getStatusCode();
                requestError = aRequestError(AUTHORISATION_ONE_TIME_TOKEN_INVALID_ERROR, exception.getConnectorErrorMessage());
                break;
            case INVALID_ATTRIBUTE_VALUE:
                errorStatus = SC_UNPROCESSABLE_ENTITY;
                requestError = aRequestError(GENERIC_VALIDATION_EXCEPTION_MESSAGE_FROM_CONNECTOR, exception.getConnectorErrorMessage());
                break;
            default:
                LOGGER.error("Connector invalid response was {}.\n Returning http status {}", exception.getConnectorErrorMessage(), INTERNAL_SERVER_ERROR);
                errorStatus = INTERNAL_SERVER_ERROR.getStatusCode();
                requestError = aRequestError(CREATE_PAYMENT_CONNECTOR_ERROR, exception.getConnectorErrorMessage());
        }
        return Response
                .status(errorStatus)
                .entity(requestError)
                .build();
    }
}
