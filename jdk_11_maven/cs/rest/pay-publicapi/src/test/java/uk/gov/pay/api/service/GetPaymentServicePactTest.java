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
import uk.gov.pay.api.model.CardPayment;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.Wallet;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.pay.api.model.links.PostLink;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GetPaymentServicePactTest {

    private static final String ACCOUNT_ID = "123456";
    private static final String CHARGE_ID = "ch_123abc456def";

    private GetPaymentService getPaymentService;

    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);

    @Mock
    private PublicApiConfig mockConfiguration;

    @Before
    public void setup() {
        when(mockConfiguration.getConnectorUrl()).thenReturn(connectorRule.getUrl());
        when(mockConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        PublicApiUriGenerator publicApiUriGenerator = new PublicApiUriGenerator(mockConfiguration);
        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(mockConfiguration);
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        getPaymentService = new GetPaymentService(publicApiUriGenerator,
                new ConnectorService(client, connectorUriGenerator),
                new LedgerService(client, ledgerUriGenerator));
    }
    
    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-get-created-payment"})
    public void testGetPayment() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID);

        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getState(), is(new PaymentState("created", false)));
        assertThat(paymentResponse.getDescription(), is("Test description"));
        assertThat(paymentResponse.getReference(), is("aReference"));
        assertThat(paymentResponse.getLanguage(), is(SupportedLanguage.ENGLISH));
        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID));
        assertThat(paymentResponse.getReturnUrl().get(), is("https://somewhere.gov.uk/rainbow/1"));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getCreatedDate(), is("2018-09-07T13:12:02.121Z"));
        assertThat(paymentResponse.getMoto(), is (false));
        assertThat(paymentResponse.getDelayedCapture(), is(true));
        assertThat(paymentResponse.getCorporateCardSurcharge(), is(Optional.empty()));
        assertThat(paymentResponse.getTotalAmount(), is(Optional.empty()));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.WEB));
        assertThat(paymentResponse.getLinks().getSelf().getHref(), containsString("v1/payments/" + CHARGE_ID));
        assertThat(paymentResponse.getLinks().getSelf().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getRefunds().getHref(), containsString("v1/payments/" + CHARGE_ID + "/refunds"));
        assertThat(paymentResponse.getLinks().getRefunds().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getNextUrl().getHref(), containsString("secure/ae749781-6562-4e0e-8f56-32d9639079dc"));
        assertThat(paymentResponse.getLinks().getNextUrl().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getNextUrlPost(), is(new PostLink(
                "https://card_frontend/secure",
                "POST",
                "application/x-www-form-urlencoded",
                Collections.singletonMap("chargeTokenId", "ae749781-6562-4e0e-8f56-32d9639079dc")
        )));
        assertThat(paymentResponse.getLinks().getCapture(), is(nullValue()));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-get-wallet-payment"})
    public void testGetPaymentWithWalletType() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID);

        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getState(), is(new PaymentState("success", true)));
        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID));
        assertThat(paymentResponse.getPaymentProvider(), is("sandbox"));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.WEB));
        assertThat(paymentResponse.getCardDetails().get().getCardHolderName(), is("aName"));
        assertThat(paymentResponse.getCardDetails().get().getWalletType().get(), is(Wallet.APPLE_PAY.getTitleCase()));
    }
    
    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-get-capturable-payment"})
    public void testGetCapturablePaymentWithMetadataAndCorporateSurcharge() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID);
        
        assertThat(paymentResponse.getProviderId(), is("gateway-tx-123456"));
        assertThat(paymentResponse.getCorporateCardSurcharge().get(), is(250L));
        assertThat(paymentResponse.getTotalAmount().get(), is(350L));
        assertThat(paymentResponse.getLinks().getCapture().getHref(),
                containsString("v1/payments/" + CHARGE_ID + "/capture"));
        assertThat(paymentResponse.getLinks().getCapture().getMethod(), is("POST"));
        assertThat(paymentResponse.getMetadata(), is(notNullValue()));
        assertThat(paymentResponse.getMetadata().getMetadata().isEmpty(), is(false));
        assertThat(paymentResponse.getLinks().getNextUrl(), is(nullValue()));
        assertThat(paymentResponse.getLinks().getNextUrlPost(), is(nullValue()));
        assertThat(paymentResponse.getAuthorisationSummary().getThreeDSecure().isRequired(), is(true));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-get-payment-with-fee-and-net-amount"})
    public void testGetPaymentWithFeeAndNetAmount() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID);

        assertThat(paymentResponse.getProviderId(), is("gateway-tx-123456"));
        assertThat(paymentResponse.getFee().get(), is(5L));
        assertThat(paymentResponse.getNetAmount().get(), is(345L));
        assertThat(paymentResponse.getLinks().getNextUrl(), is(nullValue()));
        assertThat(paymentResponse.getLinks().getNextUrlPost(), is(nullValue()));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-get-rejected-rcp-payment-with-can-retry-true"})
    public void testGetRejectedRecurringPaymentWithCanRetryTrue() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID);

        assertThat(paymentResponse.getState().getCanRetry(), is(true));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.AGREEMENT));
        assertThat(paymentResponse.getLinks().getNextUrl(), is(nullValue()));
        assertThat(paymentResponse.getLinks().getNextUrlPost(), is(nullValue()));
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-get-motoapi-created-payment"})
    public void testGetMotoApiPayment() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");

        PaymentWithAllLinks paymentResponse = getPaymentService.getPayment(account, CHARGE_ID);

        assertThat(paymentResponse.getAmount(), is(100L));
        assertThat(paymentResponse.getState(), is(new PaymentState("created", false)));
        assertThat(paymentResponse.getDescription(), is("a description"));
        assertThat(paymentResponse.getReference(), is("a reference"));
        assertThat(paymentResponse.getPaymentId(), is(CHARGE_ID));
        assertThat(paymentResponse.getMoto(), is (true));
        assertThat(paymentResponse.getAuthorisationMode(), is(AuthorisationMode.MOTO_API));
        assertThat(paymentResponse.getLinks().getSelf().getHref(), containsString("v1/payments/" + CHARGE_ID));
        assertThat(paymentResponse.getLinks().getSelf().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getRefunds().getHref(), containsString("v1/payments/" + CHARGE_ID + "/refunds"));
        assertThat(paymentResponse.getLinks().getRefunds().getMethod(), is("GET"));
        assertThat(paymentResponse.getLinks().getAuthUrlPost(), is(new PostLink("http://publicapi.test.localhost/v1/auth", "POST", "application/json", Collections.singletonMap("one_time_token", "token_1234567asdf"))));
    }
}
