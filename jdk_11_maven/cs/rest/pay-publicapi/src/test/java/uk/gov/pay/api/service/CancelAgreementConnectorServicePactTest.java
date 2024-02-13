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
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.service.payments.commons.model.ErrorIdentifier;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CancelAgreementConnectorServicePactTest {

    private final Account ACCOUNT = new Account("123456", TokenPaymentType.CARD, "a-token-link");
    
    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);

    private ConnectorService connectorService;
    @Mock
    private PublicApiConfig mockConfiguration;

    @Before
    public void setUp() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        connectorService = new ConnectorService(client, connectorUriGenerator);
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-cancel-agreement-with-active-status"})
    public void cancelAnAgreementWithActiveStatus() {
        connectorService.cancelAgreement(ACCOUNT, "agreement1234567");
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-cancel-agreement-with-cancelled-status"})
    public void cancelAnAgreementWithCancelledStatus() {
        CancelAgreementException cancelAgreementException = assertThrows(CancelAgreementException.class,
                () -> connectorService.cancelAgreement(ACCOUNT, "agreement9876543"));
        assertThat(cancelAgreementException, hasProperty("errorStatus", Matchers.is(400)));
        assertThat(cancelAgreementException, hasProperty("errorIdentifier", is(ErrorIdentifier.AGREEMENT_NOT_ACTIVE)));
        assertThat(cancelAgreementException.getConnectorErrorMessage(), is("Payment instrument not active."));
    }
    
}
