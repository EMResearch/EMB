package uk.gov.pay.api.filter.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.filter.RateLimiterKey;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RateLimiterTest {

    private static final String POST = "POST";
    private static final String accountId = "account-id";
    @Mock
    private LocalRateLimiter localRateLimiter;
    @Mock
    private RedisRateLimiter redisRateLimiter;

    private RateLimiterKey rateLimiterKey;
    private RateLimiter rateLimiter;

    @BeforeEach
    public void setup() {
        rateLimiterKey = new RateLimiterKey("key2", "key-type", POST);
        rateLimiter = new RateLimiter(localRateLimiter, redisRateLimiter);
    }

    @Test
    public void shouldInvokeRedisRateLimiter_whenRedisDbIsAvaiable() throws Exception {
        rateLimiter.checkRateOf(accountId, rateLimiterKey);
        rateLimiter.checkRateOf(accountId, rateLimiterKey);

        verify(redisRateLimiter, times(2)).checkRateOf(accountId, rateLimiterKey);
    }

    @Test
    public void shouldInvokeLocalRateLimiter_whenRedisIsNotAvaiable() throws Exception {
        doThrow(new RedisException()).when(redisRateLimiter).checkRateOf(accountId, rateLimiterKey);

        rateLimiter.checkRateOf(accountId, rateLimiterKey);
        rateLimiter.checkRateOf(accountId, rateLimiterKey);

        verify(localRateLimiter, times(2)).checkRateOf(accountId, rateLimiterKey);
    }
}
