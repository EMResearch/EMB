package uk.gov.pay.api.service;

import au.com.dius.pact.consumer.PactVerification;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
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
import uk.gov.pay.api.model.CardPayment;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.Wallet;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.service.payments.commons.testing.port.PortFactory.findFreePort;

@RunWith(MockitoJUnitRunner.class)
public class GetPaymentServiceLedgerPactTest {

    private static final String ACCOUNT_ID = "123456";
    private static final String tokenLink = "a-token-link";
    private static final String CHARGE_ID_NON_EXISTENT_IN_CONNECTOR = "ch_123abc456xyz";
    private static final int CONNECTOR_PORT = findFreePort();

    private GetPaymentService getPaymentService;

    @ClassRule
    public static WireMockClassRule connectorRule = new WireMockClassRule(CONNECTOR_PORT);

    @Rule
    public PactProviderRule ledgerRule = new PactProviderRule("ledger", this);

    @Mock
    private PublicApiConfig mockConfiguration;

    @Before
    public void setup() {
        connectorRule.resetAll();
        when(mockConfiguration.getConnectorUrl()).thenReturn("http://localhost:" + CONNECTOR_PORT);
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorRule);
        connectorMockClient.respondChargeNotFound(ACCOUNT_ID, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR, "not found");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        getPaymentService = new GetPaymentService(publicApiUriGenerator,
                new ConnectorService(client, connectorUriGenerator),
                new LedgerService(client, ledgerUriGenerator));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-metadata"})
    public void testGetPaymentWithMetadataFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);
        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getMetadata(), is(notNullValue()));
        assertThat(paymentResponse.getMetadata().getMetadata().isEmpty(), is(false));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-gateway-transaction-id"})
    public void providerIdIsAvailableWhenPaymentIsSubmitted_Ledger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);
        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getProviderId(), is("gateway-tx-123456"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-delayed-capture-true"})
    public void testGetPaymentFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);

        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getState(), is(new PaymentState("created", false)));
        assertThat(paymentResponse.getDescription(), is("Test description"));
        assertThat(paymentResponse.getReference(), is("aReference"));
        assertThat(paymentResponse.getLanguage(), is(SupportedLanguage.ENGLISH));
        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getReturnUrl().get(), is("https://somewhere.gov.uk/rainbow/1"));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getCreatedDate(), is("2018-09-07T13:12:02.121Z"));
        assertThat(paymentResponse.getDelayedCapture(), is(true));
        assertThat(paymentResponse.getCorporateCardSurcharge(), is(Optional.empty()));
        assertThat(paymentResponse.getTotalAmount(), is(Optional.empty()));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.WEB));
        assertThat(paymentResponse.getLinks().getSelf().getHref(), containsString("v1/payments/" + CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getLinks().getSelf().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getRefunds().getHref(), containsString("v1/payments/" + CHARGE_ID_NON_EXISTENT_IN_CONNECTOR + "/refunds"));
        assertThat(paymentResponse.getLinks().getRefunds().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getNextUrl(), is(nullValue()));
        assertThat(paymentResponse.getLinks().getNextUrlPost(), is(nullValue()));
        assertThat(paymentResponse.getLinks().getCapture(), is(nullValue()));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-wallet-payment"})
    public void testGetPaymentFromLedgerWithWalletType() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);

        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getState(), is(new PaymentState("capturable", false)));
        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.WEB));
        assertThat(paymentResponse.getCardDetails().get().getCardHolderName(), is("J Doe"));
        assertThat(paymentResponse.getCardDetails().get().getWalletType().get(), is(Wallet.APPLE_PAY.getTitleCase()));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-corporate-surcharge"})
    public void testGetPaymentWithCorporateCardSurchargeFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getCorporateCardSurcharge().get(), is(250L));
        assertThat(paymentResponse.getTotalAmount().get(), is(2250L));
        assertThat(paymentResponse.getLinks().getCapture(), is(nullValue()));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-fee-and-net-amount"})
    public void testGetPaymentWithFeeAndNetAmountFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);

        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID_NON_EXISTENT_IN_CONNECTOR));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getFee().get(), is(5L));
        assertThat(paymentResponse.getNetAmount().get(), is(95L));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-settled-date"})
    public void testGetPaymentWithSettledDate() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, "ch_123abc456settlement");
        assertThat(paymentResponse.getSettlementSummary().isPresent(), is(true));
        assertThat(paymentResponse.getSettlementSummary().get().getSettledDate(), is("2020-09-19"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-payment-with-authorisation-summary"})
    public void testGetPaymentWithAuthorisationSummaryFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getAuthorisationSummary().getThreeDSecure().isRequired(), is(true));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-rejected-recurring-payment-with-can-retry-true"})
    public void testGetRejectedPaymentWithNoRetryTrueFromLedger() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID_NON_EXISTENT_IN_CONNECTOR);
        assertThat(paymentResponse.getState().getCanRetry(), is(true));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.AGREEMENT));
    }
}
