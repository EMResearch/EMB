package uk.gov.pay.api.service;

import au.com.dius.pact.consumer.PactVerification;
import com.jayway.jsonassert.JsonAssert;
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
import uk.gov.pay.api.exception.SearchPaymentsException;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.search.PaginationDecorator;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CardPaymentSearchServicePactTest {

    @Rule
    public PactProviderRule ledgerRule = new PactProviderRule("ledger", this);

    @Mock
    private PublicApiConfig configuration;
    private PaymentSearchService paymentSearchService;
    private static final String tokenLink = "a-token-link";

    @Before
    public void setUp() {
        when(configuration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());

        when(configuration.getBaseUrl()).thenReturn("http://publicapi.test.localhost/");

        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(configuration);
        paymentSearchService = new PaymentSearchService(
                new PublicApiUriGenerator(configuration),
                new PaginationDecorator(configuration),
                new LedgerService(client, ledgerUriGenerator));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-payment-by-first-digits-card-number"})
    public void searchShouldReturnAResponseWithOneTransaction_whenFilteringByFirstDigitsCardNumberFromLedger() {
        Account account = new Account("123456", TokenPaymentType.CARD, tokenLink);
        var searchParams = new PaymentSearchParams.Builder()
                .withFirstDigitsCardNumber("424242")
                .withPageNumber("1")
                .withDisplaySize("500")
                .build();
        Response response = paymentSearchService.searchLedgerPayments(account, searchParams);
        JsonAssert.with(response.getEntity().toString())
                .assertThat("count", is(1))
                .assertThat("total", is(1))
                .assertThat("page", is(1))
                .assertThat("results", hasSize(equalTo(1)))
                .assertThat("results[0]", hasKey("amount"))
                .assertThat("results[0]", hasKey("state"))
                .assertThat("results[0]", hasKey("reference"))
                .assertThat("results[0]", hasKey("email"))
                .assertThat("results[0].card_details.cardholder_name", is("J Doe"))
                .assertThat("results[0].card_details", hasKey("first_digits_card_number"))
                .assertThat("results[0].card_details", hasKey("last_digits_card_number"))
                .assertThat("results[0].state", hasKey("status"))
                .assertThat("results[0].state", hasKey("finished"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-payment-by-last-digits-card-number"})
    public void searchShouldReturnAResponseWithOneTransaction_whenFilteringByLastDigitsCardNumberFromLedger() {
        Account account = new Account("123456", TokenPaymentType.CARD, tokenLink);
        var searchParams = new PaymentSearchParams.Builder()
                .withLastDigitsCardNumber("4242")
                .withPageNumber("1")
                .withDisplaySize("500")
                .build();
        Response response = paymentSearchService.searchLedgerPayments(account, searchParams);
        JsonAssert.with(response.getEntity().toString())
                .assertThat("count", is(1))
                .assertThat("total", is(1))
                .assertThat("page", is(1))
                .assertThat("results", hasSize(equalTo(1)))
                .assertThat("results[0]", hasKey("amount"))
                .assertThat("results[0]", hasKey("state"))
                .assertThat("results[0]", hasKey("reference"))
                .assertThat("results[0]", hasKey("email"))
                .assertThat("results[0].card_details.cardholder_name", is("J Doe"))
                .assertThat("results[0].card_details", hasKey("first_digits_card_number"))
                .assertThat("results[0].card_details", hasKey("last_digits_card_number"))
                .assertThat("results[0].state", hasKey("status"))
                .assertThat("results[0].state", hasKey("finished"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-payment-with-charge-in-success-state"})
    public void searchShouldReturnAResponseWithOneTransaction_whenFilteringByStateFromLedger() {
        Account account = new Account("123456", TokenPaymentType.CARD, tokenLink);
        var searchParams = new PaymentSearchParams.Builder()
                .withState("success")
                .withPageNumber("1")
                .withDisplaySize("500")
                .build();
        Response response = paymentSearchService.searchLedgerPayments(account, searchParams);
        JsonAssert.with(response.getEntity().toString())
                .assertThat("count", is(1))
                .assertThat("total", is(1))
                .assertThat("page", is(1))
                .assertThat("results", hasSize(equalTo(1)))
                .assertThat("results[0]", hasKey("amount"))
                .assertThat("results[0]", hasKey("state"))
                .assertThat("results[0]", hasKey("reference"))
                .assertThat("results[0]", hasKey("email"))
                .assertThat("results[0].card_details.cardholder_name", is("J Doe"))
                .assertThat("results[0].card_details", hasKey("first_digits_card_number"))
                .assertThat("results[0].card_details", hasKey("last_digits_card_number"))
                .assertThat("results[0].state", hasKey("status"))
                .assertThat("results[0].state", hasKey("finished"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-payments"})
    public void ledgerSearchShouldReturnAResponseWithOneTransaction() {
        Account account = new Account("123456", TokenPaymentType.CARD, tokenLink);
        var searchParams = new PaymentSearchParams.Builder()
                .withCardHolderName("j.doe@example.org")
                .build();

        Response response = paymentSearchService.searchLedgerPayments(account, searchParams);
        JsonAssert.with(response.getEntity().toString())
                .assertThat("count", is(1))
                .assertThat("total", is(1))
                .assertThat("page", is(1))
                .assertThat("results", hasSize(equalTo(1)))
                .assertThat("results[0]", hasKey("amount"))
                .assertThat("results[0]", hasKey("state"))
                .assertThat("results[0]", hasKey("reference"))
                .assertThat("results[0]", hasKey("email"))
                .assertThat("results[0].card_details.cardholder_name", is("j.doe@example.org"))
                .assertThat("results[0].card_details", hasKey("first_digits_card_number"))
                .assertThat("results[0].card_details", hasKey("last_digits_card_number"))
                .assertThat("results[0].state", hasKey("status"))
                .assertThat("results[0].state", hasKey("finished"))
                .assertThat("results[0]", hasKey("authorisation_summary"))
                .assertThat("results[0].authorisation_summary", hasKey("three_d_secure"))
                .assertThat("results[0].authorisation_summary.three_d_secure.required", is(true))
                .assertThat("results[0].authorisation_mode", is("web"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-payments-page-not-found"})
    public void shouldReturn404WhenSearchingWithNonExistentPageNumber() {
        Account account = new Account("123456", TokenPaymentType.CARD, tokenLink);
        var searchParams = new PaymentSearchParams.Builder()
                .withDisplaySize("500")
                .withPageNumber("999")
                .build();

        SearchPaymentsException searchPaymentsException = assertThrows(SearchPaymentsException.class,
                () -> paymentSearchService.searchLedgerPayments(account, searchParams));
        
        assertThat(searchPaymentsException, hasProperty("errorStatus", is(404)));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = ("publicapi-ledger-search-payments-with-settled_dates"))
    public void shouldReturnAPaymentWhenSearchedBySettledDates() {
        String accountId = "123456";
        Account account = new Account(accountId, TokenPaymentType.CARD, tokenLink);
        var searchParams = new PaymentSearchParams.Builder()
                .withFromSettledDate("2020-09-19")
                .withToSettledDate("2020-09-20")
                .withDisplaySize("500")
                .withPageNumber("1")
                .build();
        Response response = paymentSearchService.searchLedgerPayments(account, searchParams);
        JsonAssert.with(response.getEntity().toString())
                .assertThat("count", is(1))
                .assertThat("total", is(1))
                .assertThat("page", is(1))
                .assertThat("results", hasSize(equalTo(1)))
                .assertThat("results[0].settlement_summary", hasKey("settled_date"))
                .assertThat("results[0].settlement_summary.settled_date", is("2020-09-19"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = ("publicapi-ledger-search-payment-by-agreement-id"))
    public void shouldReturnAPaymentWhenSearchedByAgreementId() {
        String accountId = "123456";
        String agreementId = "agreement-1";
        Account account = new Account(accountId, TokenPaymentType.CARD, tokenLink);
        var searchParams = new PaymentSearchParams.Builder()
                .withAgreementId(agreementId)
                .withDisplaySize("500")
                .withPageNumber("1")
                .build();
        Response response = paymentSearchService.searchLedgerPayments(account, searchParams);
        JsonAssert.with(response.getEntity().toString())
                .assertThat("count", is(1))
                .assertThat("total", is(1))
                .assertThat("page", is(1))
                .assertThat("results", hasSize(equalTo(1)))
                .assertThat("results[0].authorisation_mode", is(AuthorisationMode.AGREEMENT.getName()));
    }
}
