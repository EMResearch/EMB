package uk.gov.pay.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.filter.ratelimit.RateLimitException;
import uk.gov.pay.api.filter.ratelimit.RateLimiter;
import uk.gov.pay.api.model.TokenPaymentType;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimiterFilterTest {

    public static final String ACCOUNT_ID = "account-id";
    private RateLimiterFilter rateLimiterFilter;
    private RateLimiter rateLimiter;
    @Mock
    private ContainerRequestContext mockContainerRequestContext;
    @Mock
    private UriInfo mockUriInfo;

    @BeforeEach
    public void setup() {
        rateLimiter = mock(RateLimiter.class);
        rateLimiterFilter = new RateLimiterFilter(rateLimiter, new ObjectMapper());

        when(mockContainerRequestContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getPath()).thenReturn("");
    }

    @Test
    public void shouldCheckRateLimitsWhenFilterIsInvoked() throws Exception {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "some-token-link");
        SecurityContext mockSecurityContext = mock(SecurityContext.class);
        when(mockSecurityContext.getUserPrincipal()).thenReturn(account);
        when(mockContainerRequestContext.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockContainerRequestContext.getMethod()).thenReturn("GET");
        when(mockContainerRequestContext.getMethod()).thenReturn("POST");

        rateLimiterFilter.filter(mockContainerRequestContext);

        verify(rateLimiter).checkRateOf(eq(ACCOUNT_ID), any());
    }

    @Test
    public void shouldSendErrorResponse_whenRateLimitExceeded() throws Exception {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "some-token-link");
        SecurityContext mockSecurityContext = mock(SecurityContext.class);
        when(mockSecurityContext.getUserPrincipal()).thenReturn(account);
        when(mockContainerRequestContext.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockContainerRequestContext.getMethod()).thenReturn("GET");
        doThrow(RateLimitException.class).when(rateLimiter).checkRateOf(eq("account-id"), any());

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> rateLimiterFilter.filter(mockContainerRequestContext));

        Response response = webApplicationException.getResponse();
        assertEquals(429, response.getStatus());
        assertEquals("application/json", response.getHeaderString("Content-Type"));
        assertEquals("utf-8", response.getHeaderString("Content-Encoding"));
        assertEquals("{\"code\":\"P0900\",\"description\":\"Too many requests\"}", response.getEntity());
    }

    @Test
    public void shouldNotCheckRateLimit_on_healthcheck() throws Exception {
        when(mockUriInfo.getPath()).thenReturn(("healthcheck"));
        rateLimiterFilter.filter(mockContainerRequestContext);
        verify(rateLimiter, never()).checkRateOf(anyString(), any());
    }
}
