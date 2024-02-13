package uk.gov.pay.api.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.utils.ApiKeyGenerator.apiKeyValueOf;

@ExtendWith(MockitoExtension.class)
class AuthorizationValidationFilterTest {

    private static final String SECRET_KEY = "mysupersecret";
    private AuthorizationValidationFilter authorizationValidationFilter;

    @Mock
    private PublicApiConfig mockConfiguration;
    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private HttpServletResponse mockResponse;
    @Mock
    private FilterChain mockFilterChain;
    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthorizationValidationFilter.class);
        logger.setLevel(Level.WARN);
        logger.addAppender(mockAppender);
        
        when(mockConfiguration.getApiKeyHmacSecret()).thenReturn(SECRET_KEY);
        
        authorizationValidationFilter = new AuthorizationValidationFilter(mockConfiguration);
    }

    @Test
    void shouldProcessFilterChain_whenAuthorizationHeaderIsValid() throws Exception {

        String validToken = "asdfghdasd";
        String authorization = "Bearer " + apiKeyValueOf(validToken, SECRET_KEY);

        when(mockRequest.getRequestURI()).thenReturn("/v1/payments");
        when(mockRequest.getHeader("Authorization")).thenReturn(authorization);

        authorizationValidationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void shouldProcessFilterChain_whenUrlIsAuthURLAndNoAuthorisationHeaderPresent() throws Exception {
        when(mockRequest.getRequestURI()).thenReturn("/v1/auth");
        authorizationValidationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void shouldRejectRequest_with401ResponseError_whenAuthorizationHeaderIsInvalid() throws Exception {

        String invalidApiKey = "asdfghdasdakjshdkjwhdjweghrhjgwerguweurweruhiweuiweriuui";
        String authorization = "Bearer " + invalidApiKey;

        when(mockRequest.getRequestURI()).thenReturn("/v1/payments");
        when(mockRequest.getHeader("Authorization")).thenReturn(authorization);

        authorizationValidationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verifyNoInteractions(mockFilterChain);
        verify(mockResponse).sendError(401, "Unauthorized");

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(logEvents, hasSize(1));
        assertThat(logEvents.get(0).getFormattedMessage(), is("Attempt to authenticate using an API key with an invalid checksum"));
    }

    @Test
    void shouldRejectRequest_with401ResponseError_whenAuthorizationHeaderIsNotPresent() throws Exception {

        when(mockRequest.getRequestURI()).thenReturn("/v1/payments");
        when(mockRequest.getHeader("Authorization")).thenReturn(null);

        authorizationValidationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verifyNoInteractions(mockFilterChain);
        verify(mockResponse).sendError(401, "Unauthorized");
    }

    @Test
    void shouldRejectRequest_with401ResponseError_whenAuthorizationHeaderHasInvalidFormat() throws Exception {

        String validToken = "asdfghdasd";
        String authorization = "Bearer" + apiKeyValueOf(validToken, SECRET_KEY);

        when(mockRequest.getRequestURI()).thenReturn("/v1/payments");
        when(mockRequest.getHeader("Authorization")).thenReturn(authorization);

        authorizationValidationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verifyNoInteractions(mockFilterChain);
        verify(mockResponse).sendError(401, "Unauthorized");
    }

    @Test
    void shouldRejectRequest_with401ResponseError_whenAuthorizationHeaderHasNotMinimumLengthExpected() throws Exception {

        String apiKey = RandomStringUtils.randomAlphanumeric(32);
        String authorization = "Bearer " + apiKey;

        when(mockRequest.getRequestURI()).thenReturn("/v1/payments");
        when(mockRequest.getHeader("Authorization")).thenReturn(authorization);

        authorizationValidationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verifyNoInteractions(mockFilterChain);
        verify(mockResponse).sendError(401, "Unauthorized");
    }
}
