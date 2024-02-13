package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.CreateRefundException;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static uk.gov.pay.api.model.RequestError.Code.ACCOUNT_DISABLED;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_AMOUNT_AVAILABLE_MISMATCH;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_NOT_AVAILABLE;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_NOT_AVAILABLE_DUE_TO_DISPUTE;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_NOT_FOUND_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class CreateRefundExceptionMapper implements ExceptionMapper<CreateRefundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateRefundExceptionMapper.class);

    @Override
    public Response toResponse(CreateRefundException exception) {

        RequestError requestError;
        Response.Status status;

        if (exception.getErrorStatus() == NOT_FOUND.getStatusCode()) {
            requestError = aRequestError(CREATE_PAYMENT_REFUND_NOT_FOUND_ERROR);
            status = NOT_FOUND;

        }
        else {
            switch (exception.getErrorIdentifier()) {
                case ACCOUNT_DISABLED: {
                    requestError = aRequestError(ACCOUNT_DISABLED);
                    status = FORBIDDEN;
                    break;
                }
                case REFUND_NOT_AVAILABLE: {
                    if (exception.hasReason()) {
                        requestError = aRequestError(CREATE_PAYMENT_REFUND_NOT_AVAILABLE, exception.getReason());
                        status = BAD_REQUEST;
                    }
                    else {
                        LOGGER.error("Connector response for REFUND_NOT_AVAILABLE is missing the 'reason' field");
                        requestError = aRequestError(CREATE_PAYMENT_REFUND_CONNECTOR_ERROR);
                        status = INTERNAL_SERVER_ERROR;
                    }
                    break;
                }
                case REFUND_NOT_AVAILABLE_DUE_TO_DISPUTE: {
                    requestError = aRequestError(CREATE_PAYMENT_REFUND_NOT_AVAILABLE_DUE_TO_DISPUTE);
                    status = BAD_REQUEST;
                    break;
                }
                case REFUND_AMOUNT_AVAILABLE_MISMATCH: {
                    requestError = aRequestError(CREATE_PAYMENT_REFUND_AMOUNT_AVAILABLE_MISMATCH);
                    status = PRECONDITION_FAILED;
                    break;
                }
                default: {
                    requestError = aRequestError(CREATE_PAYMENT_REFUND_CONNECTOR_ERROR);
                    status = INTERNAL_SERVER_ERROR;
                }
            }
        }

        if (status == INTERNAL_SERVER_ERROR) {
            LOGGER.info("Connector invalid response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
        }

        return Response
                .status(status)
                .entity(requestError)
                .build();
    }
}
