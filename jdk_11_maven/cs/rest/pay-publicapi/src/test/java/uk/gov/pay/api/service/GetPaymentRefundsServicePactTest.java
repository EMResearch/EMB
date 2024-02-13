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
import uk.gov.pay.api.model.RefundsResponse;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GetPaymentRefundsServicePactTest {

    private static final String ACCOUNT_ID = "123456";
    @Rule
    public PactProviderRule ledgerRule = new PactProviderRule("ledger", this);
    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);
    @Mock
    private PublicApiConfig mockConfiguration;
    private GetPaymentRefundsService getPaymentRefundsService;

    @Before
    public void setUp() {
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);

        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        LedgerService ledgerService = new LedgerService(client, ledgerUriGenerator);

        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        ConnectorService connectorService = new ConnectorService(client, connectorUriGenerator);

        getPaymentRefundsService = new GetPaymentRefundsService(connectorService, ledgerService, publicApiUriGenerator);
    }

    @Test
    @PactVerification("ledger")
    @Pacts(pacts = {"publicapi-ledger-get-payment-refunds"})
    public void shouldReturnRefundsForPaymentCorrectlyFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        RefundsResponse response = getPaymentRefundsService.getLedgerTransactionTransactions(account, "ch_123abc456xyz");
        assertThat(response.getPaymentId(), is("ch_123abc456xyz"));
        assertThat(response.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz/refunds"));
        assertThat(response.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz"));

        List<RefundResponse> refunds = response.getEmbedded().getRefunds();
        assertThat(refunds.size(), is(2));

        assertThat(refunds.get(0).getRefundId(), is("refund-transaction-id1"));
        assertThat(refunds.get(0).getStatus(), is("submitted"));
        assertThat(refunds.get(0).getAmount(), is(100L));
        assertThat(refunds.get(0).getCreatedDate(), is("2018-09-22T10:14:16.067Z"));
        assertThat(refunds.get(0).getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz"));
        assertThat(refunds.get(0).getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz/refunds/refund-transaction-id1"));

        assertThat(refunds.get(1).getRefundId(), is("refund-transaction-id2"));
        assertThat(refunds.get(1).getStatus(), is("error"));
        assertThat(refunds.get(1).getAmount(), is(200L));
        assertThat(refunds.get(1).getCreatedDate(), is("2018-09-22T10:16:16.067Z"));
        assertThat(refunds.get(1).getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz"));
        assertThat(refunds.get(1).getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/ch_123abc456xyz/refunds/refund-transaction-id2"));
    }

    @Test
    @PactVerification("connector")
    @Pacts(pacts = {"publicapi-connector-get-payment-refunds"})
    public void shouldReturnRefundsForPaymentCorrectlyFromConnector() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        RefundsResponse response = getPaymentRefundsService.getConnectorPaymentRefunds(account, "charge8133029783750222");
        assertThat(response.getPaymentId(), is("charge8133029783750222"));
        assertThat(response.getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/charge8133029783750222/refunds"));
        assertThat(response.getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/charge8133029783750222"));

        List<RefundResponse> refunds = response.getEmbedded().getRefunds();
        assertThat(refunds.size(), is(2));

        assertThat(refunds.get(0).getRefundId(), is("di0qnu9ucdo7aslhatci6h90jk"));
        assertThat(refunds.get(0).getStatus(), is("success"));
        assertThat(refunds.get(0).getAmount(), is(1L));
        assertThat(refunds.get(0).getCreatedDate(), is("2016-01-25T13:23:55.000Z"));
        assertThat(refunds.get(0).getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/charge8133029783750222"));
        assertThat(refunds.get(0).getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/charge8133029783750222/refunds/di0qnu9ucdo7aslhatci6h90jk"));

        assertThat(refunds.get(1).getRefundId(), is("m16ufgc3t23l766ljhv9eicsn5"));
        assertThat(refunds.get(1).getStatus(), is("error"));
        assertThat(refunds.get(1).getAmount(), is(1L));
        assertThat(refunds.get(1).getCreatedDate(), is("2016-01-25T16:23:55.000Z"));
        assertThat(refunds.get(1).getLinks().getPayment().getHref(), is("http://publicapi.test.localhost/v1/payments/charge8133029783750222"));
        assertThat(refunds.get(1).getLinks().getSelf().getHref(), is("http://publicapi.test.localhost/v1/payments/charge8133029783750222/refunds/m16ufgc3t23l766ljhv9eicsn5"));
    }
}
