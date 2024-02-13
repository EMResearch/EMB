package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.BadRefundsRequestException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

public class BadRefundsRequestExceptionMapper implements ExceptionMapper<BadRefundsRequestException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadRefundsRequestExceptionMapper.class);

    @Override
    public Response toResponse(BadRefundsRequestException exception) {

        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Bad Refunds Request exception {}", requestError);

        return Response.status(Status.BAD_REQUEST)
                .entity(exception.getRequestError())
                .build();
    }
}
