package uk.gov.pay.api.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.exception.SearchAgreementsException;
import uk.gov.pay.api.model.RequestError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_AGREEMENTS_LEDGER_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_AGREEMENTS_NOT_FOUND;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class SearchAgreementsExceptionMapper implements ExceptionMapper<SearchAgreementsException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchAgreementsExceptionMapper.class);

    @Override
    public Response toResponse(SearchAgreementsException exception) {
        if (exception.getErrorStatus() == NOT_FOUND.getStatusCode()) {
            return Response
                    .status(NOT_FOUND)
                    .entity(aRequestError(SEARCH_AGREEMENTS_NOT_FOUND))
                    .build();
        } else {
            RequestError requestError = aRequestError(SEARCH_AGREEMENTS_LEDGER_ERROR);
            final Response.Status status = INTERNAL_SERVER_ERROR;
            LOGGER.error("Ledger response was {}.\n Returning http status {} with error body {}", exception.getMessage(), status, requestError);
            return Response
                    .status(status)
                    .entity(requestError)
                    .build();
        }
    }

}
