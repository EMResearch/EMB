package uk.gov.pay.api.ledger.service;

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
import uk.gov.pay.api.exception.BadRequestException;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.model.TransactionSearchParams;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.search.card.PaymentForSearchResult;
import uk.gov.pay.api.service.PaymentUriGenerator;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.matcher.BadRequestExceptionMatcher.aBadRequestExceptionWithError;

@RunWith(MockitoJUnitRunner.class)
public class TransactionSearchServiceTest {

    @Mock
    private PublicApiConfig mockPublicApiConfiguration;

    private TransactionSearchService transactionSearchService;

    private static final String ACCOUNT_ID = "123456";
    private static final String CHARGE_ID = "charge97837509646393e3C";

    @Rule
    public PactProviderRule ledgerRule = new PactProviderRule("ledger", this);

    @Before
    public void setup() {
        when(mockPublicApiConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        when(mockPublicApiConfiguration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        PaymentUriGenerator paymentUriGenerator = new PaymentUriGenerator();
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockPublicApiConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));

        transactionSearchService = new TransactionSearchService(client, mockPublicApiConfiguration, ledgerUriGenerator,
                paymentUriGenerator);
    }

    @Test
    public void shouldThrowBadRequestException() {
        TransactionSearchParams searchParams = mock(TransactionSearchParams.class);
        when(searchParams.getQueryMap()).thenReturn(Map.of("not_supported", "hello"));
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> transactionSearchService.doSearch(new Account("1", TokenPaymentType.CARD, "a-token-link"), searchParams));
        assertThat(badRequestException, aBadRequestExceptionWithError("P0401",
                "Invalid parameters: not_supported. See Public API documentation for the correct data formats"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-transaction"})
    public void testSearchTransaction() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, "a-token-link");
        TransactionSearchParams searchParams = new TransactionSearchParams();

        SearchResults<PaymentForSearchResult> searchResults = transactionSearchService.doSearch(account, searchParams);

        PaymentForSearchResult payment = searchResults.getResults().get(0);

        assertThat(payment.getAmount(), is(1000L));
        assertThat(payment.getState(), is(new PaymentState("created", false)));
        assertThat(payment.getDescription(), is("Test description"));
        assertThat(payment.getReference(), is("aReference"));
        assertThat(payment.getLanguage(), is(SupportedLanguage.ENGLISH));
        assertThat(payment.getPaymentId(), is(CHARGE_ID));
        assertThat(payment.getReturnUrl().get(), is("https://example.org"));
        assertThat(payment.getEmail().get(), is("someone@example.org"));
        assertThat(payment.getPaymentProvider(), is("sandbox"));
        assertThat(payment.getCreatedDate(), is("2018-09-22T10:13:16.067Z"));
        assertThat(payment.getDelayedCapture(), is(false));

        assertThat(payment.getCardDetails().get().getCardHolderName(), is("J. Smith"));
        assertThat(payment.getCardDetails().get().getCardBrand(), is(""));

        Address address = payment.getCardDetails().get().getBillingAddress().get();
        assertThat(address.getLine1(), is("line1"));
        assertThat(address.getLine2(), is("line2"));
        assertThat(address.getPostcode(), is("AB1 2CD"));
        assertThat(address.getCity(), is("London"));
        assertThat(address.getCountry(), is("GB"));

        assertThat(payment.getLinks().getSelf().getHref(), containsString("v1/payments/" + CHARGE_ID));
        assertThat(payment.getLinks().getSelf().getMethod(), is("GET"));
        assertThat(payment.getLinks().getRefunds().getHref(), containsString("v1/payments/" + CHARGE_ID + "/refunds"));
        assertThat(payment.getLinks().getRefunds().getMethod(), is("GET"));

        assertThat(searchResults.getCount(), is(1));
        assertThat(searchResults.getTotal(), is(1));
        assertThat(searchResults.getPage(), is(1));

        assertThat(searchResults.getLinks().getSelf().getHref(), containsString("/v1/transactions?display_size=500&page=1"));
        assertThat(searchResults.getLinks().getFirstPage().getHref(), containsString("/v1/transactions?display_size=500&page=1"));
        assertThat(searchResults.getLinks().getLastPage().getHref(), containsString("/v1/transactions?display_size=500&page=1"));
    }
}
