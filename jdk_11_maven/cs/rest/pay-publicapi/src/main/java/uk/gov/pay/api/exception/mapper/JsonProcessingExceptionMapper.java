package uk.gov.pay.api.exception.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.apache.http.HttpStatus;
import uk.gov.pay.api.exception.AgreementValidationException;
import uk.gov.pay.api.exception.PaymentValidationException;
import uk.gov.pay.api.model.RequestError;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;

import static uk.gov.pay.api.model.RequestError.aRequestError;

@Priority(1)
public class JsonProcessingExceptionMapper extends LoggingExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {
        if (exception.getCause() instanceof PaymentValidationException) {
            RequestError requestError = ((PaymentValidationException) exception.getCause()).getRequestError();
            return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY).entity(requestError).build();
        } else if (exception.getCause() instanceof AgreementValidationException) {
            RequestError requestError = ((AgreementValidationException) exception.getCause()).getRequestError();
            return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY).entity(requestError).build();
        } else if (exception instanceof MismatchedInputException) {
            MismatchedInputException mismatchedInputException = (MismatchedInputException) exception;
            String typeName = isNumeric(mismatchedInputException.getTargetType()) ?
                    "numeric" :
                    mismatchedInputException.getTargetType().getSimpleName();

            String message = String.format("Must be a valid %s format", typeName);
            var requestError = aRequestError(mismatchedInputException.getPath().get(0).getFieldName(),
                    RequestError.Code.CREATE_PAYMENT_VALIDATION_ERROR,
                    message);
            
            return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY).entity(requestError).build();
        } else {
            return super.toResponse(exception);
        }
    }

    private boolean isNumeric(Class type) {
        return type == int.class || type == long.class || type == double.class || Number.class.isAssignableFrom(type);
    }

}
