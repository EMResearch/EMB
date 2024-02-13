package uk.gov.pay.api.auth;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.model.publicauth.AuthResponse;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTH_TOKEN_INVALID;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTH_TOKEN_REVOKED;

@ExtendWith(MockitoExtension.class)
public class AccountAuthenticatorTest {

    private AccountAuthenticator accountAuthenticator;
    private ObjectMapper objectMapper = new ObjectMapper();

    private final String bearerToken = "aaa";
    private final String accountId = "accountId";
    
    @Mock
    private Client publicAuthMock;
    
    @Mock
    private WebTarget mockTarget;
    
    @Mock
    private Invocation.Builder mockRequest;
    
    @Mock 
    private Response mockResponse;
    
    @Mock
    private PublicApiConfig mockConfiguration;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(AccountAuthenticator.class);
        logger.setLevel(Level.INFO);
        logger.addAppender(mockAppender);
        
        when(mockConfiguration.getPublicAuthUrl()).thenReturn("");
        accountAuthenticator = new AccountAuthenticator(publicAuthMock, mockConfiguration);
        when(publicAuthMock.target("")).thenReturn(mockTarget);
        when(mockTarget.request()).thenReturn(mockRequest);
        when(mockRequest.header(AUTHORIZATION, "Bearer " + bearerToken)).thenReturn(mockRequest);
        when(mockRequest.accept(MediaType.APPLICATION_JSON)).thenReturn(mockRequest);
        when(mockRequest.get()).thenReturn(mockResponse);
    }

    @Test
    public void shouldReturnValidAccount() {
        AuthResponse authResponse = new AuthResponse(accountId, "a-token-link", CARD);
        when(mockResponse.getStatus()).thenReturn(OK.getStatusCode());
        when(mockResponse.readEntity(AuthResponse.class)).thenReturn(authResponse);
        Optional<Account> maybeAccount = accountAuthenticator.authenticate(bearerToken);
        assertThat(maybeAccount.get().getName(), is(accountId));
        assertThat(maybeAccount.get().getAccountId(), is(accountId));
        assertThat(maybeAccount.get().getPaymentType(), is(CARD));
        
        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(logEvents, hasSize(1));
        assertThat(logEvents.get(0).getFormattedMessage(), is("Successfully authenticated using API key with token_link a-token-link"));
    }

    @Test
    public void shouldNotReturnAccount_ifUnauthorisedDueToTokenRevoked() {
        Map<String, String> responseEntity = ImmutableMap.of(
                "error_identifier", AUTH_TOKEN_REVOKED.toString(),
                "token_link", "a-token-link"
        );
        JsonNode response = objectMapper.valueToTree(responseEntity);
        when(mockResponse.getStatus()).thenReturn(UNAUTHORIZED.getStatusCode());
        when(mockResponse.readEntity(JsonNode.class)).thenReturn(response);
        Optional<Account> maybeAccount = accountAuthenticator.authenticate(bearerToken);
        assertThat(maybeAccount.isPresent(), is(false));

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(logEvents, hasSize(1));
        assertThat(logEvents.get(0).getFormattedMessage(), is("Attempt to authenticate using revoked API key with token_link a-token-link"));
    }

    @Test
    public void shouldNotReturnAccount_ifUnauthorisedDueToInvalidToken() {
        Map<String, String> responseEntity = ImmutableMap.of(
                "error_identifier", AUTH_TOKEN_INVALID.toString()
        );
        JsonNode response = objectMapper.valueToTree(responseEntity);
        when(mockResponse.getStatus()).thenReturn(UNAUTHORIZED.getStatusCode());
        when(mockResponse.readEntity(JsonNode.class)).thenReturn(response);
        Optional<Account> maybeAccount = accountAuthenticator.authenticate(bearerToken);
        assertThat(maybeAccount.isPresent(), is(false));

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(logEvents, hasSize(1));
        assertThat(logEvents.get(0).getFormattedMessage(), is("Attempt to authenticate using invalid API key with valid checksum"));
    }

    @Test
    public void shouldThrow_ifUnknownResponse() {
        when(mockResponse.getStatus()).thenReturn(NOT_FOUND.getStatusCode());
        assertThrows(ServiceUnavailableException.class, () -> accountAuthenticator.authenticate(bearerToken));
    }
}
