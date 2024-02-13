package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.InternalServerException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class InternalServerExceptionMapper implements ExceptionMapper<InternalServerException> {
    private static final Logger logger = LoggerFactory.getLogger(InternalServerExceptionMapper.class);
    @Override
    public Response toResponse(InternalServerException exception) {
        logger.error(exception.getMessage());
        RequestError error = aRequestError(CREATE_PAYMENT_CONNECTOR_ERROR);
        return Response
                .status(500)
                .entity(error)
                .build();
    }
}
