package uk.gov.pay.api.filter.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.filter.RateLimiterKey;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiter.class);

    private final LocalRateLimiter localRateLimiter;
    private final RedisRateLimiter redisRateLimiter;

    @Inject
    public RateLimiter(LocalRateLimiter localRateLimiter, RedisRateLimiter redisRateLimiter) {
        this.localRateLimiter = localRateLimiter;
        this.redisRateLimiter = redisRateLimiter;
    }

    public void checkRateOf(String accountId, RateLimiterKey key) throws RateLimitException {
        try {
            redisRateLimiter.checkRateOf(accountId, key);
        } catch (RedisException e) {
            LOGGER.warn("Exception occurred checking rate limits using RedisRateLimiter, falling back to LocalRateLimiter");
            localRateLimiter.checkRateOf(accountId, key);
        }
    }
}
