package uk.gov.pay.api.it.ledger;


import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.it.fixtures.PaymentNavigationLinksFixture;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.AuthorisationSummary;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.ThreeDSecure;
import uk.gov.pay.api.utils.ApiKeyGenerator;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;

import java.util.Map;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.it.fixtures.PaginatedTransactionSearchResultFixture.aPaginatedTransactionSearchResult;
import static uk.gov.pay.api.it.fixtures.PaymentResultBuilder.DEFAULT_AMOUNT;
import static uk.gov.pay.api.it.fixtures.PaymentResultBuilder.DEFAULT_CREATED_DATE;
import static uk.gov.pay.api.it.fixtures.PaymentResultBuilder.DEFAULT_RETURN_URL;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.aSuccessfulSearchPayment;
import static uk.gov.pay.api.utils.Urls.paymentLocationFor;
import static uk.gov.service.payments.commons.testing.port.PortFactory.findFreePort;

public class TransactionsResourceIT {

    private static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");
    private static final int LEDGER_PORT = findFreePort();
    private static final int PUBLIC_AUTH_PORT = findFreePort();
    private static final String GATEWAY_ACCOUNT_ID = "1234";

    @ClassRule
    public static WireMockClassRule ledgerMock = new WireMockClassRule(LEDGER_PORT);

    @ClassRule
    public static WireMockClassRule publicAuthMock = new WireMockClassRule(PUBLIC_AUTH_PORT);

    @Rule
    public DropwizardAppRule<PublicApiConfig> app = new DropwizardAppRule<>(
            PublicApi.class,
            resourceFilePath("config/test-config.yaml"),
            config("ledgerUrl", "http://localhost:" + LEDGER_PORT),
            config("publicAuthUrl", "http://localhost:" + PUBLIC_AUTH_PORT + "/v1/auth")
    );

    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    
    private LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);
    private PublicApiConfig configuration;
    
    @Before
    public void mapBearerTokenToAccountId() {
        configuration = app.getConfiguration();
        publicAuthMock.resetAll();
        ledgerMock.resetAll();
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }
    
    @Test
    public void shouldReturnAListOfTransactions() {
        Address billingAddress = new Address("line1", null, "AB1 CD2", "London", "GB");
        CardDetailsFromResponse cardDetails = new CardDetailsFromResponse(null, null, "J. Doe",
                null, billingAddress, "", null);
        PaymentNavigationLinksFixture fixture = new PaymentNavigationLinksFixture();
        fixture.withSelfLink("https://ledger/v1/transaction?account_id=1&reference=reference&page=1&display_size=500");
        fixture.withFirstLink("https://ledger/v1/transaction?account_id=1&reference=reference&page=1&display_size=500");
        fixture.withLastLink("https://ledger/v1/transaction?account_id=1&reference=reference&page=1&display_size=500");
        String transactions = aPaginatedTransactionSearchResult()
                .withCount(2)
                .withPage(1)
                .withTotal(2)
                .withLinks(fixture)
                .withPayments(aSuccessfulSearchPayment()
                        .withInProgressState("created")
                        .withReference("reference")
                        .withReturnUrl(DEFAULT_RETURN_URL)
                        .withCardDetails(cardDetails)
                        .withNumberOfResults(2)
                        .withEmail("j.doe@example.org")
                        .withAuthorisationSummary(new AuthorisationSummary(new ThreeDSecure(true)))
                        .getResults())
                .build();
        ledgerMockClient.respondOk_whenSearchCharges(transactions);

        searchTransactions(ImmutableMap.of("reference", "reference"))
                .statusCode(200)
                .contentType(JSON)
                .body("results[0].created_date", is(DEFAULT_CREATED_DATE))
                .body("results[0].reference", is("reference"))
                .body("results[0].email", is("j.doe@example.org"))
                .body("results[0].return_url", is(DEFAULT_RETURN_URL))
                .body("results[0].description", is("description-0"))
                .body("results[0].state.status", is("created"))
                .body("results[0].amount", is(DEFAULT_AMOUNT))
                .body("results[0].payment_provider", is("worldpay"))
                .body("results[0].payment_id", is("0"))
                .body("results[0].language", is("en"))
                .body("results[0].delayed_capture", is(false))
                .body("results[0]._links.self.method", is("GET"))
                .body("results[0]._links.self.href", is(paymentLocationFor(configuration.getBaseUrl(), "0")))
                .body("results[0]._links.events.href", is(paymentEventsLocationFor("0")))
                .body("results[0]._links.events.method", is("GET"))
                .body("results[0]._links.cancel.href", is(paymentCancelLocationFor("0")))
                .body("results[0]._links.cancel.method", is("POST"))
                .body("results[0]._links.refunds.href", is(paymentRefundsLocationFor("0")))
                .body("results[0]._links.refunds.method", is("GET"))
                .body("results[0].card_details.cardholder_name", is("J. Doe"))
                .body("results[0].card_details.expiry_date", is(nullValue()))
                .body("results[0].card_details.last_digits_card_number", is(nullValue()))
                .body("results[0].card_details.first_digits_card_number", is(nullValue()))
                .body("results[0].card_details.billing_address.line1", is("line1"))
                .body("results[0].card_details.billing_address.line2", is(nullValue()))
                .body("results[0].card_details.billing_address.postcode", is("AB1 CD2"))
                .body("results[0].card_details.billing_address.country", is("GB"))
                .body("results[0].card_details.card_brand", is(emptyString()))
                .body("results[0].authorisation_summary", is(notNullValue()))
                .body("results[0].authorisation_summary.three_d_secure", is(notNullValue()))
                .body("results[0].authorisation_summary.three_d_secure.required", is(true))
                .body("results[0].authorisation_mode", is("web"))
                .body("_links.self.href", is(expectedChargesLocationFor("?reference=reference&display_size=500&page=1")))
                .body("_links.first_page.href", is(expectedChargesLocationFor("?reference=reference&display_size=500&page=1")))
                .body("_links.last_page.href", is(expectedChargesLocationFor("?reference=reference&display_size=500&page=1")));
    }

    private ValidatableResponse searchTransactions(Map<String, String> queryParams) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .queryParams(queryParams)
                .get("/v1/transactions")
                .then();
    }

    private String expectedChargesLocationFor(String queryParams) {
        return "http://publicapi.url/v1/transactions" + queryParams;
    }

    private String paymentEventsLocationFor(String chargeId) {
        return paymentLocationFor(configuration.getBaseUrl(), chargeId) + "/events";
    }

    private String paymentRefundsLocationFor(String chargeId) {
        return paymentLocationFor(configuration.getBaseUrl(), chargeId) + "/refunds";
    }

    private String paymentCancelLocationFor(String chargeId) {
        return paymentLocationFor(configuration.getBaseUrl(), chargeId) + "/cancel";
    }
}
