package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.DisputesValidationException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DisputeValidationExceptionMapper implements ExceptionMapper<DisputesValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisputeValidationExceptionMapper.class);

    @Override
    public Response toResponse(DisputesValidationException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Dispute Validation exception {}", requestError);

        return Response.status(422)
                .entity(requestError)
                .build();
    }
}
