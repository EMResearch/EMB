package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.AgreementValidationException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class AgreementValidationExceptionMapper implements ExceptionMapper<AgreementValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgreementValidationException.class);

    @Override
    public Response toResponse(AgreementValidationException exception) {
        RequestError requestError = exception.getRequestError();
        LOGGER.debug("Agreement Validation exception {}", requestError);

        return Response.status(422)
                .entity(requestError)
                .build();
    }

}
