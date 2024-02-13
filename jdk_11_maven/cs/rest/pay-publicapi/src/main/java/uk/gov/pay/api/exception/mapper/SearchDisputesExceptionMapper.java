package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.SearchDisputesException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.GET_DISPUTE_LEDGER_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_DISPUTES_NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class SearchDisputesExceptionMapper implements ExceptionMapper<SearchDisputesException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchDisputesExceptionMapper.class);

    @Override
    public Response toResponse(SearchDisputesException exception) {
        if (exception.getErrorStatus() == NOT_FOUND.getStatusCode()) {
            return buildResponse(exception, SEARCH_DISPUTES_NOT_FOUND, NOT_FOUND);
        }
        return buildResponse(exception, GET_DISPUTE_LEDGER_ERROR, INTERNAL_SERVER_ERROR);
    }

    private Response buildResponse(SearchDisputesException exception, RequestError.Code connectorError, Response.Status status) {
        RequestError requestError = aRequestError(connectorError);
        LOGGER.info("Ledger response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
        return Response
                .status(status)
                .entity(requestError)
                .build();
    }
}
