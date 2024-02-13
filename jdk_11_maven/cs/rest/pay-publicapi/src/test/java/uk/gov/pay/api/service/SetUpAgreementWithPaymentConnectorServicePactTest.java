package uk.gov.pay.api.service;

import au.com.dius.pact.consumer.PactVerification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.app.RestClientFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.CardPayment;
import uk.gov.pay.api.model.CreateCardPaymentRequestBuilder;
import uk.gov.pay.api.model.CreatedPaymentWithAllLinks;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.links.Link;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SetUpAgreementWithPaymentConnectorServicePactTest {

    private final Account ACCOUNT = new Account("123456", TokenPaymentType.CARD, "a-token-link");
    
    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);

    private ConnectorService connectorService;

    private CreatePaymentService createPaymentService;
    
    @Mock
    private PublicApiConfig mockConfiguration;

    @Before
    public void setUp() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");
        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        connectorService = new ConnectorService(client, connectorUriGenerator);
        createPaymentService = new CreatePaymentService(client, publicApiUriGenerator, connectorUriGenerator);
    }
    
    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-create-payment-to-setup-agreement"})
    public void setUpAnAgreementWithPayment() {
        var buildRequestPayload = CreateCardPaymentRequestBuilder.builder()
                .amount(1968)
                .returnUrl("https://www.google.com")
                .reference("a-valid-reference")
                .description("a-valid-description");
        
        buildRequestPayload.setUpAgreement("i6sjhoa36s1lhtjl07vuuhbm72");
        var requestPayload = buildRequestPayload.build();
        
        CreatedPaymentWithAllLinks createdPaymentWithAllLinks = createPaymentService.create(ACCOUNT, requestPayload, null);
        PaymentWithAllLinks paymentResponse = createdPaymentWithAllLinks.getPayment();
        
        assertThat(paymentResponse.getPaymentId(), is("iinvkbkkrt8kcl0atps9q7p7cm"));
        assertThat(paymentResponse.getAmount(), is(1968L));
        assertThat(paymentResponse.getReference(), is("a-valid-reference"));
        assertThat(paymentResponse.getDescription(), is("a-valid-description"));
        assertThat(paymentResponse.getAgreementId(), is("i6sjhoa36s1lhtjl07vuuhbm72"));
        assertThat(paymentResponse.getLinks().getNextUrl(), is(new Link("http://CardFrontend/secure/efbdf987-3c91-4005-b892-9d056a4bd414", "GET")));
    }
}
