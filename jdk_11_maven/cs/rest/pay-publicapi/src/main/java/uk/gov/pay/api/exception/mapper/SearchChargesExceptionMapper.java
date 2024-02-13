package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.SearchPaymentsException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_PAYMENTS_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_PAYMENTS_NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class SearchChargesExceptionMapper implements ExceptionMapper<SearchPaymentsException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchChargesExceptionMapper.class);

    @Override
    public Response toResponse(SearchPaymentsException exception) {
        if (exception.getErrorStatus() == NOT_FOUND.getStatusCode()) {
            return Response
                    .status(NOT_FOUND)
                    .entity(aRequestError(SEARCH_PAYMENTS_NOT_FOUND))
                    .build();
        }
        else {
            RequestError requestError = aRequestError(SEARCH_PAYMENTS_CONNECTOR_ERROR);
            final Response.Status status = INTERNAL_SERVER_ERROR;
            LOGGER.error("Connector response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
            return Response
                    .status(status)
                    .entity(requestError)
                    .build();
        }
    }

}
