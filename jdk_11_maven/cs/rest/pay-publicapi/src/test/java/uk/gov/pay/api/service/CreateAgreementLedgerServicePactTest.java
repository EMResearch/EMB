package uk.gov.pay.api.service;

import au.com.dius.pact.consumer.PactVerification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.agreement.model.AgreementCreatedResponse;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.app.RestClientFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.model.CreateAgreementRequestBuilder;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.RECURRING_CARD_PAYMENTS_NOT_ALLOWED;

@RunWith(MockitoJUnitRunner.class)
public class CreateAgreementLedgerServicePactTest {

    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);

    @Mock
    private PublicApiConfig configuration;

    private ConnectorService connectorService;

    private Account account;

    @Before
    public void setup() {
        when(configuration.getConnectorUrl()).thenReturn(connectorRule.getUrl());

        Client client = RestClientFactory.buildClient(new RestClientConfig(false));

        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(configuration);
        connectorService = new ConnectorService(client, connectorUriGenerator);
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-create-agreement"})
    public void shouldCreateAgreement() {
        account = new Account("123456", TokenPaymentType.CARD, "a-token-link");
        CreateAgreementRequest agreementCreateRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder
                .builder()
                .description("Description for the paying user describing the purpose of the agreement")
                .userIdentifier("reference for the paying user")
                .reference("Service agreement reference"));

        AgreementCreatedResponse agreementResponse = connectorService.createAgreement(account, agreementCreateRequest);

        assertThat(agreementResponse.getAgreementId(), is("1jikqomeib6j18vp2i153b9dtu"));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-create-agreement-for-non-rcp-account"})
    public void createAgreementForNonRCPAccountShouldReturnError() {
        account = new Account("123456789", TokenPaymentType.CARD, "a-token-link");
        CreateAgreementRequest agreementCreateRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder
                .builder()
                .description("Description for the paying user")
                .userIdentifier("reference for the paying user")
                .reference("Service agreement reference"));

        CreateAgreementException exception = assertThrows(CreateAgreementException.class, () -> {
            connectorService.createAgreement(account, agreementCreateRequest);
        });

        assertThat(exception.getErrorIdentifier(), is(RECURRING_CARD_PAYMENTS_NOT_ALLOWED));
    }
}
