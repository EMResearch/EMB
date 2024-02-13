package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.SearchRefundsException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_REFUNDS_CONNECTOR_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_REFUNDS_NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class SearchRefundsExceptionMapper implements ExceptionMapper<SearchRefundsException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchRefundsExceptionMapper.class);

    @Override
    public Response toResponse(SearchRefundsException exception) {
        if (exception.getErrorStatus() == NOT_FOUND.getStatusCode()) {
            return buildResponse(exception, SEARCH_REFUNDS_NOT_FOUND, NOT_FOUND);
        }
        return buildResponse(exception, SEARCH_REFUNDS_CONNECTOR_ERROR, INTERNAL_SERVER_ERROR);
    }

    private Response buildResponse(SearchRefundsException exception, RequestError.Code connectorError, Response.Status status) {
        RequestError refundError = aRequestError(connectorError);
        LOGGER.info("Connector response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, refundError);
        return Response
                .status(status)
                .entity(refundError)
                .build();
    }
}
