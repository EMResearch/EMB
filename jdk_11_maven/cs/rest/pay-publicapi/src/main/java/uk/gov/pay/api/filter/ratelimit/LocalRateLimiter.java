package uk.gov.pay.api.filter.ratelimit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.RateLimiterConfig;
import uk.gov.pay.api.filter.RateLimiterKey;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class LocalRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRateLimiter.class);

    private final int noOfReqPerNode;
    private final int noOfReqForPostPerNode;
    private final int perMillis;

    private final Cache<String, RateLimit> cache;

    @Inject
    public LocalRateLimiter(RateLimiterConfig rateLimiterConfig) {
        this.noOfReqPerNode = rateLimiterConfig.getNoOfReqPerNode();
        this.noOfReqForPostPerNode = rateLimiterConfig.getNoOfReqForPostPerNode();
        this.perMillis = rateLimiterConfig.getPerMillis();
        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(perMillis, TimeUnit.MILLISECONDS)
                .build();
    }

    void checkRateOf(String accountId, RateLimiterKey rateLimiterKey) throws RateLimitException {

        RateLimit rateLimit = null;
        try {
            rateLimit = cache.get(rateLimiterKey.getKey(), () -> new RateLimit(getNoOfRequestsForMethod(rateLimiterKey.getMethod()), perMillis));
            rateLimit.updateAllowance();
        } catch (ExecutionException e) {
            //ExecutionException is thrown when the valueLoader (cache.get())  throws a checked exception.
            //We just create a new instance (RateLimit) so no exceptions will be thrown, this should never happen.
            LOGGER.error("Unexpected error creating a Rate Limiter object in cache", e);
        } catch (RateLimitException e) {
            LOGGER.info(String.format("LocalRateLimiter - Rate limit exceeded for account [%s] and method [%s] - count: %d, rate allowed: %d",
                    accountId,
                    rateLimiterKey.getMethod(),
                    rateLimit.getRequestCount(),
                    rateLimit.getNoOfReq()));

            throw e;
        }
    }

    private int getNoOfRequestsForMethod(String method) {
        if (HttpMethod.POST.equals(method)) {
            return noOfReqForPostPerNode;
        }
        return noOfReqPerNode;
    }
}
