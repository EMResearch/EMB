package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.CaptureChargeException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.CAPTURE_PAYMENT_CONNECTOR_BAD_REQUEST_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CAPTURE_PAYMENT_CONNECTOR_CONFLICT_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CAPTURE_PAYMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CAPTURE_PAYMENT_NOT_FOUND_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class CaptureChargeExceptionMapper implements ExceptionMapper<CaptureChargeException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureChargeExceptionMapper.class);

    @Override
    public Response toResponse(CaptureChargeException exception) {
        int errorStatus = exception.getErrorStatus();
        RequestError requestError;
        Response.Status status;

        if (errorStatus == NOT_FOUND.getStatusCode()) {
            requestError = aRequestError(CAPTURE_PAYMENT_NOT_FOUND_ERROR);
            status = NOT_FOUND;
        } else if (errorStatus == BAD_REQUEST.getStatusCode()) {
            requestError = aRequestError(CAPTURE_PAYMENT_CONNECTOR_BAD_REQUEST_ERROR);
            status = BAD_REQUEST;
        } else if (errorStatus == CONFLICT.getStatusCode()) {
            requestError = aRequestError(CAPTURE_PAYMENT_CONNECTOR_CONFLICT_ERROR);
            status = CONFLICT;
        } else {
            requestError = aRequestError(CAPTURE_PAYMENT_CONNECTOR_ERROR);
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
