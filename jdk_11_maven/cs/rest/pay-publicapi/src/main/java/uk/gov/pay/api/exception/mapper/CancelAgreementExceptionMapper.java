package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.CANCEL_AGREEMENT_CONNECTOR_BAD_REQUEST_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CANCEL_AGREEMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CANCEL_AGREEMENT_NOT_FOUND_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class CancelAgreementExceptionMapper implements ExceptionMapper<CancelAgreementException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelAgreementExceptionMapper.class);

    @Override
    public Response toResponse(CancelAgreementException exception) {
        int errorStatus = exception.getErrorStatus();
        RequestError requestError;
        Response.Status status;

        if (errorStatus == NOT_FOUND.getStatusCode()) {
            requestError = aRequestError(CANCEL_AGREEMENT_NOT_FOUND_ERROR);
            status = NOT_FOUND;
        } else if (errorStatus == BAD_REQUEST.getStatusCode()) {
            requestError = aRequestError(CANCEL_AGREEMENT_CONNECTOR_BAD_REQUEST_ERROR);
            status = BAD_REQUEST;
        } else {
            requestError = aRequestError(CANCEL_AGREEMENT_CONNECTOR_ERROR);
            status = INTERNAL_SERVER_ERROR;
        }

        if (status == INTERNAL_SERVER_ERROR) {
            LOGGER.error("Connector invalid response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
        }
        return Response
                .status(status)
                .entity(requestError)
                .build();
    }

}
