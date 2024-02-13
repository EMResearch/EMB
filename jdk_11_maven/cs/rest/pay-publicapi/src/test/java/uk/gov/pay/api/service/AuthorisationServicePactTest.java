package uk.gov.pay.api.service;

import au.com.dius.pact.consumer.PactVerification;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.app.RestClientFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;
import uk.gov.pay.api.exception.AuthorisationRequestException;
import uk.gov.pay.api.exception.mapper.AuthorisationRequestExceptionMapper;
import uk.gov.pay.api.model.AuthorisationRequest;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorisationServicePactTest {
    private AuthorisationService authorisationService;
    private AuthorisationRequest request;
    private AuthorisationRequestExceptionMapper mapper;

    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);

    @Mock
    private PublicApiConfig mockConfiguration;

    @Before
    public void setup() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        ConnectorUriGenerator uriGenerator = new ConnectorUriGenerator(mockConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        authorisationService = new AuthorisationService(client, uriGenerator);
        mapper = new AuthorisationRequestExceptionMapper();
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-valid-token"})
    public void testAuthorisePayment() {
        request = new AuthorisationRequest("onetime-12345-token", "4242424242424242", "123",
                "09/29", "Joe Boggs");
        Response response = authorisationService.authoriseRequest(request);
        assertThat(response.getStatus(), is(204));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-invalid-token"})
    public void testInvalidToken() {
        request = new AuthorisationRequest("invalid-token", "4242424242424242", "123",
                "09/29", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(400)));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-an-already-used-token"})
    public void testAlreadyUsedToken() {
        request = new AuthorisationRequest("onetime-12345-token", "4242424242424242", "123",
                "09/29", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(400)));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-invalid-card-number"})
    public void testInvalidCardNumber() {
        request = new AuthorisationRequest("onetime-12345-token", "0000000000000000", "123",
                "09/29", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(402)));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-authorise-moto-api-payment-with-invalid-expiry-date"})
    public void testMissingExpiryDate() {
        request = new AuthorisationRequest("onetime-12345-token", "4242424242424242", "123",
                "09/21", "Joe Boggs");
        AuthorisationRequestException authorisationRequestException = assertThrows(AuthorisationRequestException.class,
                () -> authorisationService.authoriseRequest(request));

        assertThat(authorisationRequestException, hasProperty("errorStatus", Matchers.is(422)));
    }
}
