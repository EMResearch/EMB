package uk.gov.pay.api.it;

import com.google.common.collect.ImmutableMap;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.it.fixtures.PaymentNavigationLinksFixture;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.Wallet;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.validation.DateTimeUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.http.HttpHeaders.PRAGMA;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.it.fixtures.PaginatedPaymentSearchResultFixture.aPaginatedPaymentSearchResult;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.DEFAULT_AMOUNT;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.DEFAULT_CAPTURED_DATE;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.DEFAULT_CAPTURE_SUBMIT_TIME;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.DEFAULT_CREATED_DATE;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.DEFAULT_PAYMENT_PROVIDER;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.DEFAULT_RETURN_URL;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.DEFAULT_SETTLED_DATE;
import static uk.gov.pay.api.it.fixtures.PaymentSearchResultBuilder.aSuccessfulSearchPayment;
import static uk.gov.pay.api.utils.Urls.paymentLocationFor;

public class PaymentsResourceSearchIT extends PaymentResourceITestBase {

    private static final String TEST_REFERENCE = "test_reference";
    private static final String TEST_EMAIL = "alice.111@mail.fake";
    private static final String TEST_FIRST_DIGITS_CARD_NUMBER = "123456";
    private static final String TEST_LAST_DIGITS_CARD_NUMBER = "1234";
    private static final String TEST_CARDHOLDER_NAME = "Mr. Payment";
    private static final String TEST_STATE = "created";
    private static final String TEST_CARD_BRAND_LABEL = "Mastercard";
    private static final String TEST_CARD_TYPE = "credit";
    private static final String TEST_FROM_DATE = "2016-01-28T00:00:00Z";
    private static final String TEST_TO_DATE = "2016-01-28T12:00:00Z";
    private static final String SEARCH_PATH = "/v1/payments";
    private static final Address BILLING_ADDRESS = new Address("line1", "line2", "NR2 5 6EG", "city", "UK");
    private static final CardDetailsFromResponse CARD_DETAILS = new CardDetailsFromResponse(TEST_LAST_DIGITS_CARD_NUMBER, TEST_FIRST_DIGITS_CARD_NUMBER, TEST_CARDHOLDER_NAME, "12/19", BILLING_ADDRESS, TEST_CARD_BRAND_LABEL, TEST_CARD_TYPE);

    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);

    @Before
    public void mapBearerTokenToAccountId() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void searchPaymentsWithMetadata() {
        String payments = aPaginatedPaymentSearchResult()
                .withCount(2)
                .withPage(1)
                .withTotal(2)
                .withPayments(aSuccessfulSearchPayment()
                        .withInProgressState(TEST_STATE)
                        .withReference(TEST_REFERENCE)
                        .withCardDetails(CARD_DETAILS)
                        .withNumberOfResults(2)
                        .withEmail(TEST_EMAIL)
                        .withMetadata(Map.of("reconciled", true, "ledger_code", 123, "fuh", "fuh you", "surcharge", 1.23))
                        .getResults())
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);

        searchPayments(Map.of())
                .statusCode(200)
                .header(PRAGMA, "no-cache")
                .header(CACHE_CONTROL, "no-store")
                .contentType(JSON)
                .body("results[0].metadata.reconciled", is(true))
                .body("results[0].metadata.ledger_code", is(123))
                .body("results[0].metadata.fuh", is("fuh you"))
                .body("results[0].metadata.surcharge", is(1.23f))
                .body("results[1].metadata.reconciled", is(true))
                .body("results[1].metadata.ledger_code", is(123))
                .body("results[1].metadata.fuh", is("fuh you"))
                .body("results[1].metadata.surcharge", is(1.23f));
    }

    @Test
    public void searchPayments_shouldOnlyReturnAllowedProperties() {
        String payments = aPaginatedPaymentSearchResult()
                .withCount(10)
                .withPage(2)
                .withTotal(20)
                .withPayments(aSuccessfulSearchPayment()
                        .withInProgressState(TEST_STATE)
                        .withReference(TEST_REFERENCE)
                        .withReturnUrl(DEFAULT_RETURN_URL)
                        .withCardDetails(CARD_DETAILS)
                        .withDelayedCapture(true)
                        .withNumberOfResults(1)
                        .withEmail(TEST_EMAIL)
                        .withFee(5L)
                        .withNetAmount(9995L)
                        .withGatewayTransactionId("gateway-tx-123456")
                        .getResults())
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);

        searchPayments(ImmutableMap.of("reference", TEST_REFERENCE))
                .statusCode(200)
                .contentType(JSON)
                .body("results[0].created_date", is(DEFAULT_CREATED_DATE))
                .body("results[0].reference", is(TEST_REFERENCE))
                .body("results[0].email", is(TEST_EMAIL))
                .body("results[0].return_url", is(DEFAULT_RETURN_URL))
                .body("results[0].description", is("description-0"))
                .body("results[0].state.status", is(TEST_STATE))
                .body("results[0].amount", is(DEFAULT_AMOUNT))
                .body("results[0].fee", is(5))
                .body("results[0].net_amount", is(9995))
                .body("results[0].payment_provider", is(DEFAULT_PAYMENT_PROVIDER))
                .body("results[0].payment_id", is("0"))
                .body("results[0].language", is("en"))
                .body("results[0].delayed_capture", is(true))
                .body("results[0].provider_id", is("gateway-tx-123456"))
                .body("results[0]._links.self.method", is("GET"))
                .body("results[0]._links.self.href", is(paymentLocationFor(configuration.getBaseUrl(), "0")))
                .body("results[0]._links.self", not(hasKey("type")))
                .body("results[0]._links.self", not(hasKey("params")))
                .body("results[0]._links.events.href", is(paymentEventsLocationFor("0")))
                .body("results[0]._links.events.method", is("GET"))
                .body("results[0]._links.cancel.href", is(paymentCancelLocationFor("0")))
                .body("results[0]._links.cancel.method", is("POST"))
                .body("results[0]._links.refunds.href", is(paymentRefundsLocationFor("0")))
                .body("results[0]._links.refunds.method", is("GET"))
                .body("results[0]._links", not(hasKey("next_url")))
                .body("results[0].refund_summary.status", is("available"))
                .body("results[0].refund_summary.amount_available", is(100))
                .body("results[0].refund_summary.amount_submitted", is(300))
                .body("results[0].settlement_summary.capture_submit_time", is(DEFAULT_CAPTURE_SUBMIT_TIME))
                .body("results[0].settlement_summary.captured_date", is(DEFAULT_CAPTURED_DATE))
                .body("results[0].card_details.card_brand", is(TEST_CARD_BRAND_LABEL))
                .body("results[0].card_details.cardholder_name", is(CARD_DETAILS.getCardHolderName()))
                .body("results[0].card_details.expiry_date", is(CARD_DETAILS.getExpiryDate()))
                .body("results[0].card_details.last_digits_card_number", is(CARD_DETAILS.getLastDigitsCardNumber()))
                .body("results[0].card_details.first_digits_card_number", is(CARD_DETAILS.getFirstDigitsCardNumber()))
                .body("results[0].card_details.billing_address.line1", is(CARD_DETAILS.getBillingAddress().get().getLine1()))
                .body("results[0].card_details.billing_address.line2", is(CARD_DETAILS.getBillingAddress().get().getLine2()))
                .body("results[0].card_details.billing_address.postcode", is(CARD_DETAILS.getBillingAddress().get().getPostcode()))
                .body("results[0].card_details.billing_address.country", is(CARD_DETAILS.getBillingAddress().get().getCountry()))
                .body("results[0].card_details.card_brand", is(CARD_DETAILS.getCardBrand()))
                .body("results[0].card_details", hasKey("card_type"))
                .body("results[0].metadata", is(nullValue()))
                .body("results[0].authorisation_mode", is("web"));
    }

    @Test
    public void searchPayments_ShouldNotIncludeCancelLinkIfThePaymentCannotBeCancelled() {
        String payments = aPaginatedPaymentSearchResult()
                .withCount(10)
                .withPage(2)
                .withTotal(20)
                .withPayments(aSuccessfulSearchPayment()
                        .withSuccessState("success")
                        .withReference(TEST_REFERENCE)
                        .withNumberOfResults(1)
                        .getResults())
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);

        searchPayments(ImmutableMap.of("reference", TEST_REFERENCE))
                .statusCode(200)
                .contentType(JSON)
                .body("results[0]._links.cancel", is(nullValue()));
    }

    @Test
    public void searchPayments_ShouldIncludeCanRetryIfThePaymentWasRejected() {
        String payments = aPaginatedPaymentSearchResult()
                .withCount(10)
                .withPage(2)
                .withTotal(20)
                .withPayments(aSuccessfulSearchPayment()
                        .withRejectedState("failed", "P0010", "Payment method rejected", true)
                        .withReference(TEST_REFERENCE)
                        .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                        .withNumberOfResults(1)
                        .getResults())
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);

        searchPayments(ImmutableMap.of("reference", TEST_REFERENCE))
                .statusCode(200)
                .contentType(JSON)
                .body("results[0].authorisation_mode", is("agreement"))
                .body("results[0].state.can_retry", is(true));
    }

    @Test
    public void searchPayments_getsPaginatedResults() {
        PaymentNavigationLinksFixture links = new PaymentNavigationLinksFixture()
                .withPrevLink("http://server:port/path?query=prev&from_date=2016-01-01T23:59:59Z")
                .withNextLink("http://server:port/path?query=next&from_date=2016-01-01T23:59:59Z")
                .withSelfLink("http://server:port/path?query=self&from_date=2016-01-01T23:59:59Z")
                .withFirstLink("http://server:port/path?query=first&from_date=2016-01-01T23:59:59Z")
                .withLastLink("http://server:port/path?query=last&from_date=2016-01-01T23:59:59Z");

        String payments = aPaginatedPaymentSearchResult()
                .withCount(10)
                .withPage(2)
                .withTotal(40)
                .withPayments(aSuccessfulSearchPayment()
                        .withReference(TEST_REFERENCE)
                        .withInProgressState(TEST_STATE)
                        .withCreatedDateBetween(TEST_FROM_DATE, TEST_TO_DATE)
                        .withNumberOfResults(10)
                        .withEmail(TEST_EMAIL)
                        .getResults())
                .withLinks(links)
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);

        ImmutableMap<String, String> queryParams = ImmutableMap.of(
                "reference", TEST_REFERENCE,
                "state", TEST_STATE,
                "email", TEST_EMAIL,
                "page", "2",
                "display_size", "10"
        );
        ValidatableResponse response = searchPayments(queryParams)
                .statusCode(200)
                .contentType(JSON)
                .body("results.size()", equalTo(10))
                .body("total", is(40))
                .body("count", is(10))
                .body("page", is(2))
                .body("_links.next_page.href", is(expectedChargesLocationFor("?from_date=2016-01-01T23%3A59%3A59Z&query=next")))
                .body("_links.prev_page.href", is(expectedChargesLocationFor("?from_date=2016-01-01T23%3A59%3A59Z&query=prev")))
                .body("_links.first_page.href", is(expectedChargesLocationFor("?from_date=2016-01-01T23%3A59%3A59Z&query=first")))
                .body("_links.last_page.href", is(expectedChargesLocationFor("?from_date=2016-01-01T23%3A59%3A59Z&query=last")))
                .body("_links.self.href", is(expectedChargesLocationFor("?from_date=2016-01-01T23%3A59%3A59Z&query=self")));

        List<Map<String, Object>> results = response.extract().body().jsonPath().getList("results");
        assertThat(results, matchesField("reference", TEST_REFERENCE));
        assertThat(results, matchesField("email", TEST_EMAIL));
        assertThat(results, matchesState(TEST_STATE));
        assertThat(results, matchesCreatedDateInBetween(TEST_FROM_DATE, TEST_TO_DATE));
    }

    private String expectedChargesLocationFor(String queryParams) {
        return "http://publicapi.url" + SEARCH_PATH + queryParams;
    }

    @Test
    public void searchPayments_errorIfLedgerRespondsWith404() {
        searchPayments(
                ImmutableMap.of("reference", TEST_REFERENCE, "state", TEST_STATE, "from_date", TEST_FROM_DATE, "to_date", TEST_TO_DATE))
                .statusCode(404)
                .contentType(JSON)
                .body("size()", is(2))
                .body("code", is("P0402"))
                .body("description", is("Page not found"));
    }

    @Test
    public void searchPayments_errorIfLedgerResponseIsInvalid() throws Exception {
        ledgerMockClient.whenSearchTransactions(
                aResponse().withStatus(OK_200).withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody("wtf"));

        searchPayments(
                ImmutableMap.of(
                        "reference", TEST_REFERENCE,
                        "email", TEST_EMAIL,
                        "state", TEST_STATE,
                        "from_date", TEST_FROM_DATE,
                        "to_date", TEST_TO_DATE))
                .statusCode(500)
                .contentType(JSON)
                .body("size()", is(2))
                .body("code", is("P0498"))
                .body("description", is("Downstream system error"))
                .extract();
    }

    @Test
    public void searchPayments_filterByInvalidCardBrand() throws Exception {
        searchPayments(
                ImmutableMap.of("card_brand", "my_credit_card"))
                .statusCode(404)
                .contentType(JSON)
                .body("size()", is(2))
                .body("description", is("Page not found"));
    }

    @Test
    public void searchPayments_getsResults_withNoBillingAddressAndNoWalletType() {
        String payments = aPaginatedPaymentSearchResult()
                .withPayments(aSuccessfulSearchPayment()
                        .withCardDetails(new CardDetailsFromResponse("1234",
                                "1234",
                                "Card Holder",
                                "11/21",
                                null,
                                "Visa",
                                "credit"))
                        .withWalletType(null)
                        .withNumberOfResults(1)
                        .getResults())
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);

        ImmutableMap<String, String> queryParams = ImmutableMap.of();
        searchPayments(queryParams)
                .statusCode(200)
                .contentType(JSON)
                .body("results[0].card_details.billing_address", is(nullValue()))
                .body("results[0].card_details.wallet_type", is(nullValue()))
                .body("results[0].card_details.first_digits_card_number", is("1234"));
    }

    @Test
    public void searchPayments_getsResults_withWalletType() {
        String payments = aPaginatedPaymentSearchResult()
                .withPayments(aSuccessfulSearchPayment()
                        .withWalletType(Wallet.APPLE_PAY.toString())
                        .withNumberOfResults(1)
                        .getResults())
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);

        ImmutableMap<String, String> queryParams = ImmutableMap.of();
        searchPayments(queryParams)
                .statusCode(200)
                .contentType(JSON)
                .body("results[0]", not(hasKey("wallet_type")))
                .body("results[0].card_details", hasKey("wallet_type"))
                .body("results[0].card_details.wallet_type", is(Wallet.APPLE_PAY.getTitleCase()));
    }
    
    @Test
    public void shouldReturnEmptyArray_ifLedgerReturnsNoResult() {
        String payments = aPaginatedPaymentSearchResult()
                .withPage(1)
                .withPayments(aSuccessfulSearchPayment()
                        .withNumberOfResults(0)
                        .getResults())
                .build();

        ledgerMockClient.respondOk_whenSearchCharges(payments);
        
        searchPayments(ImmutableMap.of(
                "reference", "junk yard",
                "email", TEST_EMAIL,
                "state", TEST_STATE,
                "from_date", TEST_FROM_DATE,
                "to_date", TEST_TO_DATE,
                "agreement_id", "does_not_exist"))
                .statusCode(200)
                .contentType(JSON)
                .body("results.size()", is(0))
                .body("total", is(0))
                .body("count", is(0))
                .body("page", is(1));
    }

    @Test
    public void shouldReturnSettledDate_whenLedgerReturnsSettledDateInSettlementSummary() {
        String payments = aPaginatedPaymentSearchResult()
                .withPage(1)
                .withPayments(aSuccessfulSearchPayment()
                        .withSettlementSummary(new PaymentSettlementSummary(DEFAULT_CAPTURE_SUBMIT_TIME,
                                DEFAULT_CAPTURED_DATE, DEFAULT_SETTLED_DATE))
                        .withNumberOfResults(1)
                        .getResults())
                .build();
        ledgerMockClient.respondOk_whenSearchCharges(payments);

        searchPayments(Map.of()).statusCode(200)
                .contentType(JSON)
                .body("results[0].settlement_summary.settled_date", is(DEFAULT_SETTLED_DATE))
                .body("results[0].settlement_summary.captured_date", is(DEFAULT_CAPTURED_DATE))
                .body("results[0].settlement_summary.capture_submit_time", is(DEFAULT_CAPTURE_SUBMIT_TIME));
    }

    private Matcher<? super List<Map<String, Object>>> matchesState(final String state) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(List<Map<String, Object>> maps) {
                return maps.stream().allMatch(result -> state.equals(((Map<String, Object>) result.get("state")).get("status")));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("result state did not match %s", state));
            }
        };
    }

    private Matcher<? super List<Map<String, Object>>> matchesField(final String field, final String value) {
        return new TypeSafeMatcher<List<Map<String, Object>>>() {
            @Override
            protected boolean matchesSafely(List<Map<String, Object>> maps) {
                return maps.stream().allMatch(result -> value.equals(result.get(field)));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("not all result %s match %s", field, value));
            }
        };
    }

    private Matcher<? super List<Map<String, Object>>> matchesCreatedDateInBetween(final String fromDate, final String toDate) {
        return new TypeSafeMatcher<List<Map<String, Object>>>() {
            @Override
            protected boolean matchesSafely(List<Map<String, Object>> results) {
                return results.stream().allMatch(result -> {
                            ZonedDateTime createdDate = zonedDateTimeOf(result.get("created_date").toString());
                            return createdDate.isAfter(zonedDateTimeOf(fromDate)) && createdDate.isBefore(zonedDateTimeOf(toDate));
                        }
                );
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("created date of results does not fall in between %s and %s", fromDate, toDate));
            }
        };
    }

    private ZonedDateTime zonedDateTimeOf(String dateString) {
        return DateTimeUtils.toUTCZonedDateTime(dateString).get();
    }

    private ValidatableResponse searchPayments(Map<String, String> queryParams) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .queryParams(queryParams)
                .get(SEARCH_PATH)
                .then();
    }

}
