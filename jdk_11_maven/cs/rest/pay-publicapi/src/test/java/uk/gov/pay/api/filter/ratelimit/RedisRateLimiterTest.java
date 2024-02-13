package uk.gov.pay.api.filter.ratelimit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.dropwizard.setup.Environment;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.RateLimiterConfig;
import uk.gov.pay.api.filter.RateLimiterKey;
import uk.gov.pay.api.managed.RedisClientManager;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedisRateLimiterTest {

    private static final String accountId = "account-id";
    private static final Long perSecondTimeToLiveInSeconds = 1L;
    private static final Long perMinuteTimeToLiveInSeconds = 60L;

    @Mock
    private RedisClientManager redisClientManager;

    @Mock
    private StatefulRedisConnection statefulRedisConnection;

    @Mock
    private RedisCommands redisCommands;

    @Mock
    private RateLimiterKey rateLimiterKey;

    @Mock
    private RateLimiterConfig rateLimiterConfig;

    @Mock
    private Environment environment;

    @Captor
    ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    private RedisRateLimiter redisRateLimiter;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Mock
    private MetricRegistry metricsRegistry;

    private Timer timer;

    @BeforeEach
    public void setup() {
        timer = new Timer();
        when(statefulRedisConnection.sync()).thenReturn(redisCommands);
        when(redisClientManager.getRedisConnection()).thenReturn(statefulRedisConnection);
        when(environment.metrics()).thenReturn(metricsRegistry);
        when(metricsRegistry.timer(any())).thenReturn(timer);

        Logger root = (Logger) LoggerFactory.getLogger(RedisRateLimiter.class);
        root.setLevel(Level.INFO);
        root.addAppender(mockAppender);
    }

    @Test
    public void rateLimiterSetTo_1CallPerSecond_shouldAllowSingleCall() throws Exception {
        when(rateLimiterConfig.getNoOfReq()).thenReturn(1);
        when(rateLimiterKey.getKey()).thenReturn("Key1");
        when(rateLimiterConfig.getPerMillis()).thenReturn(1000);
        redisRateLimiter = new RedisRateLimiter(rateLimiterConfig, redisClientManager, environment);

        when(redisCommands.incr(anyString())).thenReturn(1L);
        when(redisCommands.expire(anyString(), eq(perSecondTimeToLiveInSeconds))).thenReturn(true);
        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        verify(redisCommands).expire(anyString(), eq(perSecondTimeToLiveInSeconds));
    }

    @Test
    public void rateLimiterSetTo_2CallsPerSecond_shouldAllow2ConsecutiveCallsWithSameKeys() throws Exception {
        when(rateLimiterConfig.getNoOfReq()).thenReturn(2);
        when(rateLimiterKey.getKey()).thenReturn("Key2");
        when(rateLimiterConfig.getPerMillis()).thenReturn(1000);
        redisRateLimiter = new RedisRateLimiter(rateLimiterConfig, redisClientManager, environment);

        when(redisCommands.incr(anyString())).thenReturn(1L, 2L);
        when(redisCommands.expire(anyString(), eq(perSecondTimeToLiveInSeconds))).thenReturn(true);

        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        verify(redisCommands, times(2)).expire(anyString(), eq(perSecondTimeToLiveInSeconds));
    }

    @Test
    public void rateLimiterSetTo_2CallsPerSecond_shouldFailWhen3ConsecutiveCallsWithSameKeysAreMade() throws RedisException, RateLimitException {
        when(rateLimiterConfig.getNoOfReq()).thenReturn(2);
        when(rateLimiterKey.getKey()).thenReturn("Key3");
        when(rateLimiterKey.getKeyType()).thenReturn("POST");
        when(rateLimiterConfig.getPerMillis()).thenReturn(1000);
        redisRateLimiter = new RedisRateLimiter(rateLimiterConfig, redisClientManager, environment);

        when(redisCommands.incr(anyString())).thenReturn(1L, 2L, 3L);
        when(redisCommands.expire(anyString(), eq(perSecondTimeToLiveInSeconds))).thenReturn(true);

        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);

        assertThrows(RateLimitException.class, () -> {
            redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
            verify(redisCommands, times(3)).expire(anyString(), eq(perSecondTimeToLiveInSeconds));
            verify(mockAppender, times(1)).doAppend(loggingEventArgumentCaptor.capture());
            List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
            assertEquals("RedisRateLimiter - Rate limit exceeded for account [account-id] and method [POST] - count: 3, rate allowed: 2",
                    loggingEvents.get(0).getFormattedMessage());
        });
    }

    @Test
    public void shouldRateLimitPostRequestsForLowTrafficAccountsCorrectly() throws RedisException, RateLimitException {
        when(rateLimiterConfig.getLowTrafficAccounts()).thenReturn(List.of(accountId));
        when(rateLimiterConfig.getNoOfPostReqForLowTrafficAccounts()).thenReturn(3);
        when(rateLimiterConfig.getIntervalInMillisForLowTrafficAccounts()).thenReturn(60000);
        when(rateLimiterKey.getMethod()).thenReturn("POST");
        when(rateLimiterKey.getKeyType()).thenReturn("POST-capture-account1");

        redisRateLimiter = new RedisRateLimiter(rateLimiterConfig, redisClientManager, environment);

        when(redisCommands.incr(anyString())).thenReturn(1L, 2L, 3L, 4L);
        when(redisCommands.expire(anyString(), eq(perMinuteTimeToLiveInSeconds))).thenReturn(true);

        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        verify(redisCommands, times(3)).expire(anyString(), eq(perMinuteTimeToLiveInSeconds));

        assertThrows("Excepted to throw exception when rate limit exceeds", RateLimitException.class, () -> {
            redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
        });
    }
    @Test
    public void shouldRateLimitGetRequestsForLowTrafficAccountsCorrectly() throws RedisException, RateLimitException {
        when(rateLimiterConfig.getLowTrafficAccounts()).thenReturn(List.of(accountId));
        when(rateLimiterConfig.getNoOfReqForLowTrafficAccounts()).thenReturn(1);
        when(rateLimiterConfig.getIntervalInMillisForLowTrafficAccounts()).thenReturn(60000);
        when(rateLimiterKey.getMethod()).thenReturn("GET");
        when(rateLimiterKey.getKeyType()).thenReturn("GET-account1");

        redisRateLimiter = new RedisRateLimiter(rateLimiterConfig, redisClientManager, environment);

        when(redisCommands.incr(anyString())).thenReturn(1L, 2L);
        when(redisCommands.expire(anyString(), eq(perMinuteTimeToLiveInSeconds))).thenReturn(true);

        redisRateLimiter.checkRateOf(accountId, rateLimiterKey);

        assertThrows("Excepted to throw exception when rate limit exceeds", RateLimitException.class, () -> {
            redisRateLimiter.checkRateOf(accountId, rateLimiterKey);
            verify(redisCommands, times(3)).expire(anyString(), eq(perMinuteTimeToLiveInSeconds));
        });
    }
}
