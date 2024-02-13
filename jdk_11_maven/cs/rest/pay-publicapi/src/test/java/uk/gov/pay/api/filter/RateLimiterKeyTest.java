package uk.gov.pay.api.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimiterKeyTest {

    @Mock
    private ContainerRequestContext containerRequestContext;

    @Mock
    private UriInfo uriInfo;

    @BeforeEach
    public void setUp() {
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    static Stream<Arguments> rateLimitParams() {
        return Stream.of(
                arguments("/v1/payments", "POST", "POST-create_payment", "POST-create_payment-account_id"),
                arguments("/v1/payments/paymentId/capture", "POST", "POST-capture_payment", "POST-capture_payment-account_id"),
                arguments("/v1/payments/paymentId/cancel", "POST", "POST", "POST-account_id"),
                arguments("/v1/payments", "GET", "GET", "GET-account_id")
        );
    }

    @ParameterizedTest
    @MethodSource("rateLimitParams")
    public void returnsRateLimiterKey(String path, String method, String expectedKeyType, String expectedKey) {
        when(uriInfo.getPath()).thenReturn(path);
        when(containerRequestContext.getMethod()).thenReturn(method);

        var rateLimiterKey = RateLimiterKey.from(containerRequestContext, "account_id");
        assertThat(rateLimiterKey.getKey(), is(expectedKey));
        assertThat(rateLimiterKey.getKeyType(), is(expectedKeyType));
    }
}
