package uk.gov.pay.api.filter.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.config.RateLimiterConfig;
import uk.gov.pay.api.filter.RateLimiterKey;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimitManagerTest {

    @Mock
    private RateLimiterConfig rateLimiterConfig;

    private RateLimitManager rateLimitManager;

    @Test
    public void returnsNumberOfAllowedRequestsForNoAccount() {
        var rateLimiterKey = new RateLimiterKey("path", "key-type", "PUT");
        when(rateLimiterConfig.getNoOfReq()).thenReturn(1);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getAllowedNumberOfRequests(rateLimiterKey, ""), is(1));
    }

    @Test
    public void returnsNumberOfAllowedRequestsForPostForAccount1() {
        var rateLimiterKey = new RateLimiterKey("path", "key-type", "POST");
        when(rateLimiterConfig.getElevatedAccounts()).thenReturn(List.of("1"));
        when(rateLimiterConfig.getNoOfPostReqForElevatedAccounts()).thenReturn(4);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getAllowedNumberOfRequests(rateLimiterKey, "1"), is(4));
    }

    @Test
    public void returnsNumberOfAllowedRequestsForGetForAccount1() {
        var rateLimiterKey = new RateLimiterKey("path", "key-type", "GET");
        when(rateLimiterConfig.getElevatedAccounts()).thenReturn(List.of("1"));
        when(rateLimiterConfig.getNoOfReqForElevatedAccounts()).thenReturn(3);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getAllowedNumberOfRequests(rateLimiterKey, "1"), is(3));
    }

    @Test
    public void returnsNumberOfAllowedRequestsForPostForAccount2() {
        var rateLimiterKey = new RateLimiterKey("path", "key-type", "POST");
        when(rateLimiterConfig.getNoOfReqForPost()).thenReturn(2);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getAllowedNumberOfRequests(rateLimiterKey, "2"), is(2));
    }

    @Test
    public void returnsNumberOfAllowedRequestsForGetForAccount2() {
        var rateLimiterKey = new RateLimiterKey("path", "key-type", "GET");
        when(rateLimiterConfig.getNoOfReq()).thenReturn(1);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getAllowedNumberOfRequests(rateLimiterKey, "2"), is(1));
    }

    @Test
    public void shouldReturnNumberOfAllowedPostRequestsCorrectlyForLowTrafficAccounts() {
        var rateLimiterKey = new RateLimiterKey("path", "key-type", "POST");
        when(rateLimiterConfig.getLowTrafficAccounts()).thenReturn(List.of("10"));
        when(rateLimiterConfig.getNoOfPostReqForLowTrafficAccounts()).thenReturn(7);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getAllowedNumberOfRequests(rateLimiterKey, "10"), is(7));
    }

    @Test
    public void shouldReturnNumberOfAllowedGetRequestsCorrectlyForLowTrafficAccounts() {
        var rateLimiterKey = new RateLimiterKey("path", "key-type", "GET");
        when(rateLimiterConfig.getLowTrafficAccounts()).thenReturn(List.of("10"));
        when(rateLimiterConfig.getNoOfReqForLowTrafficAccounts()).thenReturn(100);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getAllowedNumberOfRequests(rateLimiterKey, "10"), is(100));
    }

    @Test
    public void shouldReturnRateLimitIntervalCorrectlyForLowTrafficAccounts() {
        when(rateLimiterConfig.getLowTrafficAccounts()).thenReturn(List.of("10"));
        when(rateLimiterConfig.getIntervalInMillisForLowTrafficAccounts()).thenReturn(54000);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getRateLimitInterval("10"), is(54000));
    }

    @Test
    public void shouldReturnRateLimitIntervalCorrectlyForElevatedAndStandardRateLimiting() {
        when(rateLimiterConfig.getPerMillis()).thenReturn(10000);

        rateLimitManager = new RateLimitManager(rateLimiterConfig);
        assertThat(rateLimitManager.getRateLimitInterval("12345"), is(10000));
    }

}
