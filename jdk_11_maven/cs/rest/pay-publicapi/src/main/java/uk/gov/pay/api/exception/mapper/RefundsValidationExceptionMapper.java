package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.RefundsValidationException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class RefundsValidationExceptionMapper implements ExceptionMapper<RefundsValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundsValidationExceptionMapper.class);

    @Override
    public Response toResponse(RefundsValidationException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Refunds Validation exception {}", requestError);

        return Response.status(422)
                .entity(requestError)
                .build();
    }
}
