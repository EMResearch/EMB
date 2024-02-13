package uk.gov.pay.api.filter.ratelimit;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LocalRateLimiterTest {

    private static final String POST = "POST";
    private static final String accountId = "account-id";
    LocalRateLimiter localRateLimiter;

    @Captor
    ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @Mock
    private Appender<ILoggingEvent> mockAppender;
    
    @Mock
    private RateLimiterConfig rateLimiterConfig;

    @BeforeEach
    public void setup() {
        Logger root = (Logger) LoggerFactory.getLogger(LocalRateLimiter.class);
        root.setLevel(Level.INFO);
        root.addAppender(mockAppender);
    }

    @Test
    public void rateLimiterSetTo_2CallsPerSecond_shouldAllow2ConsecutiveCallsWithSameKeys() throws Exception {
        when(rateLimiterConfig.getNoOfReqPerNode()).thenReturn(2);
        when(rateLimiterConfig.getNoOfReqForPostPerNode()).thenReturn(2);
        when(rateLimiterConfig.getPerMillis()).thenReturn(1000);
        
        String key = "key1";
        var rateLimiterKey = new RateLimiterKey(key, "key-type", POST);
        localRateLimiter = new LocalRateLimiter(rateLimiterConfig);

        localRateLimiter.checkRateOf(accountId, rateLimiterKey);
        localRateLimiter.checkRateOf(accountId, rateLimiterKey);
    }

    @Test
    public void rateLimiterSetTo_1CallPer300Millis_shouldAFailWhen2ConsecutiveCallsWithSameKeysAreMade() throws RateLimitException {
        when(rateLimiterConfig.getNoOfReqPerNode()).thenReturn(1);
        when(rateLimiterConfig.getNoOfReqForPostPerNode()).thenReturn(1);
        when(rateLimiterConfig.getPerMillis()).thenReturn(300);
        
        String key = "key2";
        var rateLimiterKey = new RateLimiterKey(key, "key-type", POST);
        localRateLimiter = new LocalRateLimiter(rateLimiterConfig);

        localRateLimiter.checkRateOf(accountId, rateLimiterKey);
        assertThrows(RateLimitException.class, () -> {
            localRateLimiter.checkRateOf(accountId, rateLimiterKey);
            verify(mockAppender, times(1)).doAppend(loggingEventArgumentCaptor.capture());
            List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
            assertEquals("LocalRateLimiter - Rate limit exceeded for account [account-id] and method [POST] - count: 2, rate allowed: 1",
                    loggingEvents.get(0).getFormattedMessage());
        });
    }

    @Test
    public void rateLimiterSetTo_3CallsPerSecond_shouldAllowMakingOnly3CallsWithSameKey() throws Exception {
        when(rateLimiterConfig.getNoOfReqPerNode()).thenReturn(3);
        when(rateLimiterConfig.getNoOfReqForPostPerNode()).thenReturn(3);
        when(rateLimiterConfig.getPerMillis()).thenReturn(1000);
        
        String key = "key3";
        var rateLimiterKey = new RateLimiterKey(key, "key-type", POST);
        localRateLimiter = new LocalRateLimiter(rateLimiterConfig);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<String>> tasks = Arrays.asList(
                () -> {
                    localRateLimiter.checkRateOf(accountId, rateLimiterKey);
                    return "task1";
                },
                () -> {
                    localRateLimiter.checkRateOf(accountId, rateLimiterKey);
                    return "task2";
                },
                () -> {
                    localRateLimiter.checkRateOf(accountId, rateLimiterKey);
                    return "task3";
                },
                () -> {
                    localRateLimiter.checkRateOf(accountId, rateLimiterKey);
                    return "task4";
                }
        );

        List<String> successfulTasks = executor.invokeAll(tasks)
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        assertThat(e, is(instanceOf(ExecutionException.class)));
                        ExecutionException ex = (ExecutionException) e;
                        assertThat(ex.getCause(), is(instanceOf(RateLimitException.class)));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        assertThat(successfulTasks.size(), is(3));
    }


}
