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
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GetPaymentRefundServicePactTest {

    private static final String ACCOUNT_ID = "123456";
    private static final String CHARGE_ID = "123456789";
    private static final String REFUND_ID = "r_123abc456def";

    private GetPaymentRefundService getPaymentService;

    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);
    
    @Rule
    public PactProviderRule ledgerRule = new PactProviderRule("ledger", this);

    @Mock
    private PublicApiConfig mockConfiguration;

    @Before
    public void setup() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        getPaymentService = new GetPaymentRefundService(new ConnectorService(client, connectorUriGenerator),
                new LedgerService(client, ledgerUriGenerator), publicApiUriGenerator);
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-get-payment-refund"})
    public void testGetPayment() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        RefundResponse refund = getPaymentService.getConnectorPaymentRefund(account, CHARGE_ID, REFUND_ID);

        assertThat(refund.getRefundId(), is(REFUND_ID));
        assertThat(refund.getStatus(), is("success"));
        assertThat(refund.getAmount(), is(100L));
        assertThat(refund.getCreatedDate(), is("2018-09-22T10:14:16.067Z"));
        assertThat(refund.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789"));
        assertThat(refund.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789/refunds/r_123abc456def"));
    }
    
    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-refund"})
    public void testGetLedgerPaymentRefund() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        RefundResponse refund = getPaymentService.getLedgerPaymentRefund(account, CHARGE_ID, REFUND_ID);

        assertThat(refund.getRefundId(), is(REFUND_ID));
        assertThat(refund.getStatus(), is("success"));
        assertThat(refund.getAmount(), is(100L));
        assertThat(refund.getCreatedDate(), is("2018-09-22T10:14:16.067Z"));
        assertThat(refund.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789"));
        assertThat(refund.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/123456789/refunds/r_123abc456def"));
    }
}
