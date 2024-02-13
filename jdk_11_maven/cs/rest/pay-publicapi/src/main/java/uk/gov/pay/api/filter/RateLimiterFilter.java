package uk.gov.pay.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.filter.ratelimit.RateLimitException;
import uk.gov.pay.api.filter.ratelimit.RateLimiter;
import uk.gov.pay.api.resources.error.ApiErrorResponse.Code;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Optional;

import static uk.gov.pay.api.resources.error.ApiErrorResponse.anApiErrorResponse;

/**
 * Allow only a certain number of requests from the same source (given by the Authorization Header)
 * within the given time configured in the RateLimiter. See {@link RateLimiter}
 * <p>
 * 429 Too Many Requests will be returned when rate limit is reached.
 */
@Provider
@Priority(Priorities.USER + 1000)
public class RateLimiterFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterFilter.class);
    private static final int TOO_MANY_REQUESTS_STATUS_CODE = 429;
    private static final String UTF8_CHARACTER_ENCODING = "utf-8";

    private final RateLimiter rateLimiter;
    private ObjectMapper objectMapper;

    /**
     * @param rateLimiter Limiter in number of requests per given time coming from the same source (Authorization)
     */
    @Inject
    public RateLimiterFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    private void setTooManyRequestsError() throws IOException {
        String errorResponse = objectMapper.writeValueAsString(anApiErrorResponse(Code.TOO_MANY_REQUESTS_ERROR));
        Response.ResponseBuilder builder = Response.status(TOO_MANY_REQUESTS_STATUS_CODE)
                .entity(errorResponse)
                .encoding(UTF8_CHARACTER_ENCODING)
                .variant(new Variant(MediaType.APPLICATION_JSON_TYPE, "", UTF8_CHARACTER_ENCODING));

        throw new WebApplicationException(builder.build());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("healthcheck".equals(requestContext.getUriInfo().getPath())) {
            return;
        }

        String accountId = getAccountId(requestContext);
        RateLimiterKey key = RateLimiterKey.from(requestContext, accountId);
        try {
            rateLimiter.checkRateOf(accountId, key);
        } catch (RateLimitException e) {
            LOGGER.info("Rate limit reached for current service [account - {}, method - {}]. Sending response '429 Too Many Requests'",
                    accountId, key.getMethod());
            setTooManyRequestsError();
        }
    }

    private String getAccountId(ContainerRequestContext requestContext) {
        Account account = (Account) requestContext.getSecurityContext().getUserPrincipal();
        return Optional.ofNullable(account)
                .map(Account::getAccountId)
                .orElse(StringUtils.EMPTY);
    }
}
