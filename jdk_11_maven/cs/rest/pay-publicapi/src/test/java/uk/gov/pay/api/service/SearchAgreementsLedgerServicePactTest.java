package uk.gov.pay.api.service;

import au.com.dius.pact.consumer.PactVerification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.agreement.model.AgreementLedgerResponse;
import uk.gov.pay.api.app.RestClientFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchAgreementsLedgerServicePactTest {
    @Rule
    public PactProviderRule ledgerRule = new PactProviderRule("ledger", this);
    @Mock
    private PublicApiConfig mockConfiguration;

    private LedgerService ledgerService;

    private static final String LEDGER_SERVICE_URL = "http://ledger.service.backend/";

    @Before
    public void setUp() {
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());

        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);

        ledgerService = new LedgerService(client, ledgerUriGenerator);
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-agreements-with-page-and-display-size"})
    public void searchWithPageAndDisplaySize_shouldReturnCorrectPageAndPaginationLinks() {
        AgreementSearchParams params = new AgreementSearchParams(null, null, "2", "1");
        Account account = new Account("777", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);

        assertThat(results.getResults().size(), is(1));
        assertThat(results.getCount(), is(1));
        assertThat(results.getTotal(), is(3));
        assertThat(results.getPage(), is(2));
        assertThat(results.getLinks().getSelf().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=2"));
        assertThat(results.getLinks().getFirstPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=1"));
        assertThat(results.getLinks().getLastPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=3"));
        assertThat(results.getLinks().getPrevPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=1"));
        assertThat(results.getLinks().getNextPage().getHref(), is(LEDGER_SERVICE_URL + "v1/agreement?account_id=777&display_size=1&page=3"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-agreements-with-status"})
    public void searchWithStatus_shouldReturnCorrectAgreement() {
        AgreementSearchParams params = new AgreementSearchParams(null, "active", "1", "20");
        Account account = new Account("777", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);
        assertThat(results.getResults().size(), is(1));
        assertThat(results.getResults().get(0).getStatus(), is("ACTIVE"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-agreements-with-reference"})
    public void searchWithReference_shouldReturnCorrectAgreement() {
        AgreementSearchParams params = new AgreementSearchParams("a-valid-reference", null, "1", "20");
        Account account = new Account("3456", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);
        assertThat(results.getResults().size(), is(1));
        assertThat(results.getResults().get(0).getReference(), is("a-valid-reference"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-search-agreement-not-found"})
    public void getOneAgreementNotFound() {
        AgreementSearchParams params = new AgreementSearchParams("invalid-reference", null, "1", "20");
        Account account = new Account("3456", TokenPaymentType.CARD, "a-token-link");
        SearchResults<AgreementLedgerResponse> results = ledgerService.searchAgreements(account, params);
        assertThat(results.getResults().size(), is(0));
        assertThat(results.getCount(), is(0));
        assertThat(results.getTotal(), is(0));
    }
}
