package uk.gov.pay.api.it;

import com.jayway.jsonassert.JsonAssert;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.AuthorisationSummary;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.model.ThreeDSecure;
import uk.gov.pay.api.model.Wallet;
import uk.gov.pay.api.utils.ChargeEventBuilder;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ChargeResponseFromConnector;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;
import uk.gov.pay.api.utils.mocks.TransactionFromLedgerFixture;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.validation.DateTimeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.http.HttpHeaders.PRAGMA;
import static org.apache.http.HttpStatus.SC_NOT_ACCEPTABLE;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.Urls.paymentLocationFor;
import static uk.gov.pay.api.utils.mocks.ChargeResponseFromConnector.ChargeResponseFromConnectorBuilder.aCreateOrGetChargeResponseFromConnector;
import static uk.gov.pay.api.utils.mocks.TransactionEventFixture.TransactionEventFixtureBuilder.aTransactionEventFixture;
import static uk.gov.pay.api.utils.mocks.TransactionFromLedgerFixture.TransactionFromLedgerBuilder.aTransactionFromLedgerFixture;
import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class PaymentsResourceGetIT extends PaymentResourceITestBase {

    private static final ZonedDateTime CAPTURED_DATE = ZonedDateTime.parse("2016-01-02T14:03:00Z");
    private static final ZonedDateTime CAPTURE_SUBMIT_TIME = ZonedDateTime.parse("2016-01-02T15:02:00Z");
    private static final ZonedDateTime SETTLED_DATE = ZonedDateTime.parse("2016-01-06T15:02:00Z");
    private static final PaymentSettlementSummary SETTLEMENT_SUMMARY_CONNECTOR = new PaymentSettlementSummary(ISO_INSTANT_MILLISECOND_PRECISION.format(CAPTURE_SUBMIT_TIME),
            DateTimeUtils.toLocalDateString(CAPTURED_DATE), null);
    private static final PaymentSettlementSummary SETTLEMENT_SUMMARY_LEDGER = new PaymentSettlementSummary(ISO_INSTANT_MILLISECOND_PRECISION.format(CAPTURE_SUBMIT_TIME),
            DateTimeUtils.toLocalDateString(CAPTURED_DATE), DateTimeUtils.toLocalDateString(SETTLED_DATE));
    private static final int AMOUNT = 9999999;
    private static final Long FEE = 5L;
    private static final Long NET_AMOUNT = 9999994L;
    private static final String CHARGE_ID = "ch_ab2341da231434l";
    private static final String CHARGE_TOKEN_ID = "token_1234567asdf";
    private static final PaymentState CREATED = new PaymentState("created", false, null, null);
    private static final PaymentState CAPTURED = new PaymentState("captured", false, null, null);
    public static final PaymentState AWAITING_CAPTURE_REQUEST = new PaymentState("submitted", false, null, null);
    public static final PaymentState REJECTED = new PaymentState("failed", true, "Payment method rejected", "P0010", true);
    private static final RefundSummary REFUND_SUMMARY = new RefundSummary("pending", 100L, 50L);
    private static final String PAYMENT_PROVIDER = "Sandbox";
    private static final String CARD_BRAND_LABEL = "Mastercard";
    private static final String CARD_TYPE = "debit";
    private static final String RETURN_URL = "https://somewhere.gov.uk/rainbow/1";
    private static final String REFERENCE = "Some reference <script> alert('This is a ?{simple} XSS attack.')</script>";
    private static final String EMAIL = "alice.111@mail.fake";
    private static final String GATEWAY_TRANSACTION_ID = "gateway-tx-123456";
    private static final String DESCRIPTION = "Some description <script> alert('This is a ?{simple} XSS attack.')</script>";
    private static final String CREATED_DATE = ISO_INSTANT_MILLISECOND_PRECISION.format(ZonedDateTime.parse("2010-12-31T22:59:59.132012345Z"));
    private static final Map<String, String> PAYMENT_CREATED = new ChargeEventBuilder(CREATED, CREATED_DATE).build();
    private static final List<Map<String, String>> EVENTS = Collections.singletonList(PAYMENT_CREATED);
    private static final Address BILLING_ADDRESS = new Address("line1", "line2", "NR2 5 6EG", "city", "UK");
    private static final CardDetailsFromResponse CARD_DETAILS = new CardDetailsFromResponse("1234", "123456", "Mr. Payment", "12/19", BILLING_ADDRESS, CARD_BRAND_LABEL, CARD_TYPE);

    private final ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    private final PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private final LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);

    @Before
    public void setUp() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void getPaymentWithMetadataThroughConnector() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withMetadata(Map.of("reconciled", true, "ledger_code", 123, "fuh", "fuh you", "surcharge", 1.23))
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID);

        response.header(PRAGMA, "no-cache").header(CACHE_CONTROL, "no-store");
        assertCommonPaymentFields(response);
        assertConnectorOnlyPaymentFields(response);
        assertPaymentWithMetadata(response);
    }

    @Test
    public void getPaymentWithMetadataThroughLedger() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withMetadata(Map.of("reconciled", true, "ledger_code", 123, "fuh", "fuh you", "surcharge", 1.23))
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY);

        response.header(PRAGMA, "no-cache").header(CACHE_CONTROL, "no-store");
        assertCommonPaymentFields(response);
        assertPaymentWithMetadata(response);
    }

    private void assertPaymentWithMetadata(ValidatableResponse response) {
        response
                .body("metadata.reconciled", is(true))
                .body("metadata.ledger_code", is(123))
                .body("metadata.fuh", is("fuh you"))
                .body("metadata.surcharge", is(1.23f));
    }

    @Test
    public void getPaymentWithMotoThroughConnector() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withMoto(true)
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID);

        assertCommonPaymentFields(response);
        assertConnectorOnlyPaymentFields(response);
        assertPaymentWithMoto(response);
    }

    @Test
    public void getPaymentWithMotoThroughLedger() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withMoto(true)
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY);

        assertCommonPaymentFields(response);
        assertPaymentWithMoto(response);
    }

    private void assertPaymentWithMoto(ValidatableResponse response) {
        response.body("moto", is(true));
    }

    private void assertPaymentWithAuthorisationSummary(ValidatableResponse response) {
        response.body("authorisation_summary", is(notNullValue()));
        response.body("authorisation_summary.three_d_secure", is(notNullValue()));
        response.body("authorisation_summary.three_d_secure.required", is(true));
    }

    @Test
    public void getPaymentThroughConnector_ReturnsPayment() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID, getConnectorCharge().build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID);

        assertCommonPaymentFields(response);
        assertConnectorOnlyPaymentFields(response);
        response.body("metadata", is(nullValue()));
    }

    @Test
    public void getPaymentThroughLedger_ReturnsPayment() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID, getLedgerTransaction().build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY);

        assertCommonPaymentFields(response);
        response.body("metadata", is(nullValue()));
        response.body("settlement_summary.settled_date", is(DateTimeUtils.toLocalDateString(SETTLED_DATE)));
        response.body("settlement_summary.captured_date", is(DateTimeUtils.toLocalDateString(CAPTURED_DATE)));
        response.body("settlement_summary.capture_submit_time", is(ISO_INSTANT_MILLISECOND_PRECISION.format(CAPTURE_SUBMIT_TIME)));
    }

    @Test
    public void getPaymentWithAuthorisationModeMotoApiThroughConnector_ReturnsPayment() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID, getConnectorCharge()
                .withAuthorisationMode(AuthorisationMode.MOTO_API)
                .build(), true);

        ValidatableResponse response = getPaymentResponse(CHARGE_ID);

        assertCommonPaymentFields(response);
        assertConnectorOnlyMotoApiPaymentFields(response);
        response.body("authorisation_mode", is("moto_api"));
    }

    @Test
    public void getPaymentWithAuthorisationModeMotoApiThroughLedger_ReturnsPayment() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID, getLedgerTransaction()
                .withAuthorisationMode(AuthorisationMode.MOTO_API)
                .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY);

        assertCommonPaymentFields(response);
        response.body("authorisation_mode", is("moto_api"));
    }

    private void assertCommonPaymentFields(ValidatableResponse paymentResponse) {
        paymentResponse
                .statusCode(200)
                .contentType(JSON)
                .body("payment_id", is(CHARGE_ID))
                .body("reference", is(REFERENCE))
                .body("email", is(EMAIL))
                .body("description", is(DESCRIPTION))
                .body("amount", is(AMOUNT))
                .body("state.status", is(CAPTURED.getStatus()))
                .body("return_url", is(RETURN_URL))
                .body("payment_provider", is(PAYMENT_PROVIDER))
                .body("card_brand", is(CARD_BRAND_LABEL))
                .body("created_date", is(CREATED_DATE))
                .body("language", is("en"))
                .body("delayed_capture", is(true))
                .body("provider_id", is(GATEWAY_TRANSACTION_ID))
                .body("refund_summary.status", is("pending"))
                .body("refund_summary.amount_submitted", is(50))
                .body("refund_summary.amount_available", is(100))
                .body("settlement_summary.capture_submit_time", is(ISO_INSTANT_MILLISECOND_PRECISION.format(CAPTURE_SUBMIT_TIME)))
                .body("settlement_summary.captured_date", is(DateTimeUtils.toLocalDateString(CAPTURED_DATE)))
                .body("card_details.card_brand", is(CARD_BRAND_LABEL))
                .body("card_details.card_type", is(CARD_TYPE))
                .body("card_details.cardholder_name", is(CARD_DETAILS.getCardHolderName()))
                .body("card_details.first_digits_card_number", is(CARD_DETAILS.getFirstDigitsCardNumber()))
                .body("card_details.last_digits_card_number", is(CARD_DETAILS.getLastDigitsCardNumber()))
                .body("card_details.expiry_date", is(CARD_DETAILS.getExpiryDate()))
                .body("card_details.billing_address.line1", is(CARD_DETAILS.getBillingAddress().get().getLine1()))
                .body("card_details.billing_address.line2", is(CARD_DETAILS.getBillingAddress().get().getLine2()))
                .body("card_details.billing_address.postcode", is(CARD_DETAILS.getBillingAddress().get().getPostcode()))
                .body("card_details.billing_address.country", is(CARD_DETAILS.getBillingAddress().get().getCountry()))
                .body("_links.self.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)))
                .body("_links.self.method", is("GET"))
                .body("_links.events.href", is(paymentEventsLocationFor(CHARGE_ID)))
                .body("_links.events.method", is("GET"))
                .body("_links.cancel.href", is(paymentCancelLocationFor(CHARGE_ID)))
                .body("_links.cancel.method", is("POST"))
                .body("_links.refunds.href", is(paymentRefundsLocationFor(CHARGE_ID)))
                .body("_links.refunds.method", is("GET"))
                .body("containsKey('corporate_card_surcharge')", is(false))
                .body("containsKey('total_amount')", is(false));
    }

    private void assertConnectorOnlyPaymentFields(ValidatableResponse paymentResponse) {
        paymentResponse
                .body("_links.next_url.href", is(frontendUrlFor(CARD) + CHARGE_ID))
                .body("_links.next_url.method", is("GET"))
                .body("_links.next_url_post.href", is(frontendUrlFor(CARD)))
                .body("_links.next_url_post.method", is("POST"))
                .body("_links.next_url_post.type", is("application/x-www-form-urlencoded"))
                .body("_links.next_url_post.params.chargeTokenId", is(CHARGE_TOKEN_ID));
    }

    private void assertConnectorOnlyMotoApiPaymentFields(ValidatableResponse paymentResponse) {
        paymentResponse
                .body("_links.auth_url_post.href", is("http://publicapi.url/v1/auth"))
                .body("_links.auth_url_post.method", is("POST"))
                .body("_links.auth_url_post.type", is("application/json"))
                .body("_links.auth_url_post.params.one_time_token", is(CHARGE_TOKEN_ID));
    }
    
    @Test
    public void getPaymentThroughConnector_DoesNotReturnCardDigits_IfNotPresentInCardDetails() {
        CardDetailsFromResponse cardDetails = new CardDetailsFromResponse(null, null, "Mr. Payment", "12/19", BILLING_ADDRESS, CARD_BRAND_LABEL, CARD_TYPE);

        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withCardDetails(cardDetails)
                        .build());

        assertPaymentWithoutCardDetails(getPaymentResponse(CHARGE_ID));
    }

    @Test
    public void getPaymentThroughLedger_DoesNotReturnCardDigits_IfNotPresentInCardDetails() {
        CardDetailsFromResponse cardDetails = new CardDetailsFromResponse(null, null, "Mr. Payment", "12/19", BILLING_ADDRESS, CARD_BRAND_LABEL, CARD_TYPE);

        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withCardDetails(cardDetails)
                        .build());

        assertPaymentWithoutCardDetails(getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY));
    }

    private void assertPaymentWithoutCardDetails(ValidatableResponse paymentResponse) {
        paymentResponse
                .statusCode(200)
                .contentType(JSON)
                .body("payment_id", is(CHARGE_ID))
                .body("card_details.first_digits_card_number", is(nullValue()))
                .body("card_details.last_digits_card_number", is(nullValue()));
    }

    @Test
    public void getPaymentThroughConnector_ShouldNotIncludeCancelLinkIfPaymentCannotBeCancelled() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withState(new PaymentState("success", true, null, null))
                        .withSettlementSummary(null)
                        .build());

        assertPaymentWithoutCancelLink(getPaymentResponse(CHARGE_ID));
    }

    @Test
    public void getPaymentThroughLedger_ShouldNotIncludeCancelLinkIfPaymentCannotBeCancelled() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withState(new PaymentState("success", true, null, null))
                        .withSettlementSummary(null)
                        .build());

        assertPaymentWithoutCancelLink(getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY));
    }

    private void assertPaymentWithoutCancelLink(ValidatableResponse paymentResponse) {
        paymentResponse
                .statusCode(200)
                .contentType(JSON)
                .body("_links.cancel", is(nullValue()));
    }

    @Test
    public void getPaymentThroughConnector_ShouldNotIncludeSettlementFieldsIfNull() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withState(CREATED)
                        .withSettlementSummary(new PaymentSettlementSummary(null, null, null))
                        .build());

        assertPaymentWithoutSettlementSummary(getPaymentResponse(CHARGE_ID));
    }

    @Test
    public void getPaymentThroughLedger_ShouldNotIncludeSettlementFieldsIfNull() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withState(CREATED)
                        .withSettlementSummary(new PaymentSettlementSummary(null, null, null))
                        .build());

        assertPaymentWithoutSettlementSummary(getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY));
    }

    private void assertPaymentWithoutSettlementSummary(ValidatableResponse paymentResponse) {
        paymentResponse
                .statusCode(200)
                .contentType(JSON)
                .rootPath("settlement_summary")
                .body("containsKey('capture_submit_time')", is(false))
                .body("containsKey('captured_date')", is(false));
    }

    @Test
    public void getPayment_BillingAddressShouldBeNullWhenNotPresentInConnectorResponse() {
        getPayment_BillingAddressShouldBeNullWhenNotPresentInServiceResponse(
                cd -> connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                        getConnectorCharge()
                                .withCardDetails(cd)
                                .build()),
                CONNECTOR_ONLY_STRATEGY);
    }

    @Test
    public void getPayment_BillingAddressShouldBeNullWhenNotPresentInLedgerResponse() {
        getPayment_BillingAddressShouldBeNullWhenNotPresentInServiceResponse(
                cd -> ledgerMockClient.respondWithTransaction(CHARGE_ID,
                        getLedgerTransaction()
                                .withCardDetails(cd)
                                .build()),
                LEDGER_ONLY_STRATEGY);
    }

    private void getPayment_BillingAddressShouldBeNullWhenNotPresentInServiceResponse(Consumer<CardDetailsFromResponse> mockResponseFunction, String strategy) {
        CardDetailsFromResponse cardDetails = new CardDetailsFromResponse("1234",
                "123456",
                "Mr. Payment",
                "12/19",
                null,
                CARD_BRAND_LABEL,
                CARD_TYPE);

        mockResponseFunction.accept(cardDetails);

        getPaymentResponse(CHARGE_ID, strategy)
                .statusCode(200)
                .contentType(JSON)
                .body("card_details", hasKey("billing_address"))
                .body("card_details.billing_address", is(nullValue()))
                .body("payment_id", is(CHARGE_ID));
    }

    @Test
    public void getPayment_Returns401_WhenUnauthorised() {
        publicAuthMockClient.respondUnauthorised();

        getPaymentResponse(CHARGE_ID)
                .statusCode(401);
    }

    @Test
    public void getPayment_returns404_whenConnectorAndLedgerRespondWith404() throws IOException {
        String paymentId = "ds2af2afd3df112";
        String errorMessage = "backend-error-message";
        connectorMockClient.respondChargeNotFound(GATEWAY_ACCOUNT_ID, paymentId, errorMessage);
        ledgerMockClient.respondTransactionNotFound(paymentId, errorMessage);

        InputStream body = getPaymentResponse(paymentId, "future-behaviour")
                .statusCode(404)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0200"))
                .assertThat("$.description", is("Not found"));
    }

    @Test
    public void getPayment_returns500_whenConnectorRespondsWithResponseOtherThan200Or404() throws IOException {
        String paymentId = "ds2af2afd3df112";
        String errorMessage = "backend-error-message";
        connectorMockClient.respondWhenGetCharge(GATEWAY_ACCOUNT_ID, paymentId, errorMessage, SC_NOT_ACCEPTABLE);

        assertErrorPaymentResponse(getPaymentResponse(paymentId, CONNECTOR_ONLY_STRATEGY));
    }

    @Test
    public void getPayment_returns500_whenLedgerRespondsWithResponseOtherThan200Or404() throws IOException {
        String paymentId = "ds2af2afd3df112";
        String errorMessage = "backend-error-message";
        ledgerMockClient.respondTransactionWithError(paymentId, errorMessage, SC_NOT_ACCEPTABLE);

        assertErrorPaymentResponse(getPaymentResponse(paymentId, LEDGER_ONLY_STRATEGY));
    }

    private void assertErrorPaymentResponse(ValidatableResponse response) throws IOException {
        InputStream body = response
                .statusCode(500)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0298"))
                .assertThat("$.description", is("Downstream system error"));
    }

    @Test
    public void getPaymentEventsThroughConnector_ReturnsPaymentEvents() {
        connectorMockClient.respondWithChargeEventsFound(GATEWAY_ACCOUNT_ID, CHARGE_ID, EVENTS);

        assertPaymentEventsResponse(getPaymentEventsResponse(CHARGE_ID));
    }

    @Test
    public void getPaymentEventsThroughLedger_ReturnsPaymentEvents() {
        var eventFixture = aTransactionEventFixture().withState(CREATED).withTimestamp(CREATED_DATE).build();
        ledgerMockClient.respondWithTransactionEvents(CHARGE_ID, eventFixture);

        assertPaymentEventsResponse(getPaymentEventsResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY));
    }

    private void assertPaymentEventsResponse(ValidatableResponse paymentEventsResponse) {
        paymentEventsResponse
                .statusCode(200)
                .contentType(JSON)
                .body("payment_id", is(CHARGE_ID))
                .body("_links.self.href", is(paymentEventsLocationFor(CHARGE_ID)))
                .body("events", hasSize(1))
                .body("events[0].payment_id", is(CHARGE_ID))
                .body("events[0].state.status", is(CREATED.getStatus()))
                .body("events[0].updated", is("2010-12-31T22:59:59.132Z"))
                .body("events[0]._links.payment_url.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)));
    }

    @Test
    public void getPaymentEvents_Returns401_WhenUnauthorised() {
        publicAuthMockClient.respondUnauthorised();

        getPaymentEventsResponse(CHARGE_ID)
                .statusCode(401);
    }

    @Test
    public void getPaymentEvents_returns404_whenConnectorRespondsWith404() throws IOException {
        String paymentId = "ds2af2afd3df112";
        String errorMessage = "backend-error-message";
        connectorMockClient.respondChargeEventsNotFound(GATEWAY_ACCOUNT_ID, paymentId, errorMessage);

        assertEventsNotFoundResponse(getPaymentEventsResponse(paymentId));
    }

    @Test
    public void getPaymentEvents_returns404_whenLedgerRespondsWith404() throws IOException {
        String paymentId = "ds2af2afd3df112";
        String errorMessage = "backend-error-message";
        ledgerMockClient.respondTransactionEventsWithError(paymentId, errorMessage, SC_NOT_FOUND);

        assertEventsNotFoundResponse(getPaymentEventsResponse(paymentId, LEDGER_ONLY_STRATEGY));
    }

    private void assertEventsNotFoundResponse(ValidatableResponse response) throws IOException {
        InputStream body = response
                .statusCode(404)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0300"))
                .assertThat("$.description", is("Not found"));
    }

    @Test
    public void getPaymentEvents_returns500_whenConnectorRespondsWithResponseOtherThan200Or404() throws IOException {
        String paymentId = "ds2af2afd3df112";
        String errorMessage = "backend-error-message";
        connectorMockClient.respondWhenGetChargeEvents(GATEWAY_ACCOUNT_ID, paymentId, errorMessage, SC_NOT_ACCEPTABLE);

        assertErrorEventsResponse(getPaymentEventsResponse(paymentId, CONNECTOR_ONLY_STRATEGY));
    }

    @Test
    public void getPaymentEvents_returns500_whenLedgerRespondsWithResponseOtherThan200Or404() throws IOException {
        String paymentId = "ds2af2afd3df112";
        String errorMessage = "backend-error-message";
        ledgerMockClient.respondTransactionEventsWithError(paymentId, errorMessage, SC_NOT_ACCEPTABLE);

        assertErrorEventsResponse(getPaymentEventsResponse(paymentId, LEDGER_ONLY_STRATEGY));
    }

    private void assertErrorEventsResponse(ValidatableResponse response) throws IOException {
        InputStream body = response
                .statusCode(500)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0398"))
                .assertThat("$.description", is("Downstream system error"));
    }

    @Test
    public void getPaymentThroughConnector_ReturnsPaymentWithCorporateCardSurcharge() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withCorporateCardSurcharge(250L)
                        .withTotalAmount(AMOUNT + 250L)
                        .build());

        assertPaymentCorporateCardSurcharge(getPaymentResponse(CHARGE_ID));
    }

    @Test
    public void getPaymentThroughLedger_ReturnsPaymentWithCorporateCardSurcharge() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withCorporateCardSurcharge(250L)
                        .withTotalAmount(AMOUNT + 250L)
                        .build());

        assertPaymentCorporateCardSurcharge(getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY));
    }

    private void assertPaymentCorporateCardSurcharge(ValidatableResponse paymentResponse) {
        paymentResponse
                .statusCode(200)
                .contentType(JSON)
                .body("corporate_card_surcharge", is(250))
                .body("total_amount", is(AMOUNT + 250));
    }

    @Test
    public void getPaymentThroughConnector_ReturnsPaymentWithFeeAndNetAmount() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withFee(FEE)
                        .withNetAmount(NET_AMOUNT)
                        .build());

        assertPaymentWithFeeAndNetAmount(getPaymentResponse(CHARGE_ID));
    }

    @Test
    public void getPaymentThroughLedger_ReturnsPaymentWithFeeAndNetAmount() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withFee(FEE)
                        .withNetAmount(NET_AMOUNT)
                        .build());

        assertPaymentWithFeeAndNetAmount(getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY));
    }

    private void assertPaymentWithFeeAndNetAmount(ValidatableResponse paymentResponse) {
        paymentResponse
                .statusCode(200)
                .contentType(JSON)
                .body("fee", is(FEE.intValue()))
                .body("net_amount", is(NET_AMOUNT.intValue()))
                .body("amount", is(AMOUNT));
    }

    @Test
    public void getPayment_ReturnsPaymentWithOutFeeAndNetAmount_IfNotAvailableFromConnector() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID, getConnectorCharge().build());

        assertPaymentWithoutFeeAndNetAmount(getPaymentResponse(CHARGE_ID));
    }

    @Test
    public void getPaymentThroughLedger_ReturnsPaymentWithOutFeeAndNetAmount_IfNotAvailable() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID, getLedgerTransaction().build());

        assertPaymentWithoutFeeAndNetAmount(getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY));
    }

    private void assertPaymentWithoutFeeAndNetAmount(ValidatableResponse paymentResponse) {
        paymentResponse
                .statusCode(200)
                .contentType(JSON)
                .body("containsKey('fee')", is(false))
                .body("containsKey('net_amount')", is(false))
                .body("amount", is(AMOUNT));
    }

    @Test
    public void getPaymentWithNullCardTypeThroughConnector_ReturnsPaymentWithNullCardType() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withState(AWAITING_CAPTURE_REQUEST)
                        .withCardDetails(new CardDetailsFromResponse("1234", "123456", "Mr. Payment", "12/19", BILLING_ADDRESS, CARD_BRAND_LABEL, null))
                        .withCorporateCardSurcharge(0L)
                        .withTotalAmount(0L)
                        .build());

        getPaymentResponse(CHARGE_ID)
                .statusCode(200)
                .contentType(JSON)
                .body("card_details.card_type", is(nullValue()));
    }

    @Test
    public void getPaymentWithNullCardTypeThroughLedger_ReturnsPaymentWithNullCardType() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withState(AWAITING_CAPTURE_REQUEST)
                        .withCardDetails(new CardDetailsFromResponse("1234", "123456", "Mr. Payment", "12/19", BILLING_ADDRESS, CARD_BRAND_LABEL, null))
                        .withCorporateCardSurcharge(0L)
                        .withTotalAmount(0L)
                        .build());

        getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY)
                .statusCode(200)
                .contentType(JSON)
                .body("card_details.card_type", is(nullValue()));
    }

    @Test
    public void getPaymentThroughConnector_ReturnsPaymentWithCaptureUrl() { // only through connector (based on response links)
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withState(AWAITING_CAPTURE_REQUEST)
                        .withCorporateCardSurcharge(0L)
                        .withTotalAmount(0L)
                        .build());

        getPaymentResponse(CHARGE_ID)
                .statusCode(200)
                .contentType(JSON)
                .body("_links.capture.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID) + "/capture"))
                .body("_links.capture.method", is("POST"));
    }

    @Test
    public void getPaymentWithAuthorisationSummaryThroughLedger() {
        AuthorisationSummary authorisationSummary = new AuthorisationSummary(new ThreeDSecure(true));
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withAuthorisationSummary(authorisationSummary)
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY);

        assertCommonPaymentFields(response);
        assertPaymentWithAuthorisationSummary(response);
    }

    @Test
    public void getPaymentWithAuthorisationSummaryThroughConnector() {
        AuthorisationSummary authorisationSummary = new AuthorisationSummary(new ThreeDSecure(true));
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withAuthorisationSummary(authorisationSummary)
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID);

        assertCommonPaymentFields(response);
        assertConnectorOnlyPaymentFields(response);
        assertPaymentWithAuthorisationSummary(response);
    }

    @Test
    public void getPaymentWithWalletTypeThroughLedger() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withWalletType(Wallet.GOOGLE_PAY.toString())
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY);

        // PublicAPI expected response behaviour is to move wallet type into card_details
        response.body(not(hasKey("wallet_type")));
        response.body("card_details.wallet_type", is(Wallet.GOOGLE_PAY.getTitleCase()));
    }

    @Test
    public void getPaymentWithWalletTypeThroughConnector() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withWalletType(Wallet.APPLE_PAY.toString())
                        .build());
 
        ValidatableResponse response = getPaymentResponse(CHARGE_ID);
        
        assertConnectorOnlyPaymentFields(response);
        // PublicAPI expected response behaviour is to move wallet type into card_details
        response.body(not(hasKey("wallet_type")));
        response.body("card_details.wallet_type", is(Wallet.APPLE_PAY.getTitleCase()));
    }

    @Test
    public void getPaymentWithNoAuthorisationSummaryThroughConnector() {
        AuthorisationSummary authorisationSummary = new AuthorisationSummary(new ThreeDSecure(true));
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .build());

        ValidatableResponse response = getPaymentResponse(CHARGE_ID);

        assertCommonPaymentFields(response);
        assertConnectorOnlyPaymentFields(response);
        response.body("authorisation_summary", is(nullValue()));
    }

    @Test
    public void getPaymentShouldIncludeCanRetryIfThePaymentWasRejected() {
        ledgerMockClient.respondWithTransaction(CHARGE_ID,
                getLedgerTransaction()
                        .withState(REJECTED)
                        .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                        .build());

        getPaymentResponse(CHARGE_ID, LEDGER_ONLY_STRATEGY)
                .statusCode(200)
                .contentType(JSON)
                .body("authorisation_mode", is("agreement"))
                .body("state.can_retry", is(true));
    }

    @Test
    public void getRejectedPaymentShouldIncludeCanRetryWhenThroughConnector() {
        connectorMockClient.respondWithChargeFound(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID,
                getConnectorCharge()
                        .withState(REJECTED)
                        .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                        .build());

        getPaymentResponse(CHARGE_ID)
                .statusCode(200)
                .contentType(JSON)
                .body("authorisation_mode", is("agreement"))
                .body("state.can_retry", is(true));
    }

    private ChargeResponseFromConnector.ChargeResponseFromConnectorBuilder getConnectorCharge() {
        return aCreateOrGetChargeResponseFromConnector()
                .withAmount(AMOUNT)
                .withChargeId(CHARGE_ID)
                .withState(CAPTURED)
                .withReturnUrl(RETURN_URL)
                .withDescription(DESCRIPTION)
                .withReference(REFERENCE)
                .withEmail(EMAIL)
                .withPaymentProvider(PAYMENT_PROVIDER)
                .withCreatedDate(CREATED_DATE)
                .withLanguage(SupportedLanguage.ENGLISH)
                .withDelayedCapture(true)
                .withMoto(false)
                .withRefundSummary(REFUND_SUMMARY)
                .withSettlementSummary(SETTLEMENT_SUMMARY_CONNECTOR)
                .withCardDetails(CARD_DETAILS)
                .withGatewayTransactionId(GATEWAY_TRANSACTION_ID);
    }

    private TransactionFromLedgerFixture.TransactionFromLedgerBuilder getLedgerTransaction() {
        return aTransactionFromLedgerFixture()
                .withAmount((long) AMOUNT)
                .withTransactionId(CHARGE_ID)
                .withState(CAPTURED)
                .withReturnUrl(RETURN_URL)
                .withDescription(DESCRIPTION)
                .withReference(REFERENCE)
                .withEmail(EMAIL)
                .withPaymentProvider(PAYMENT_PROVIDER)
                .withCreatedDate(CREATED_DATE)
                .withLanguage(SupportedLanguage.ENGLISH)
                .withDelayedCapture(true)
                .withMoto(false)
                .withRefundSummary(REFUND_SUMMARY)
                .withSettlementSummary(SETTLEMENT_SUMMARY_LEDGER)
                .withCardDetails(CARD_DETAILS)
                .withGatewayTransactionId(GATEWAY_TRANSACTION_ID);
    }

    private ValidatableResponse getPaymentResponse(String paymentId) {
        return getPaymentResponse(paymentId, "");
    }

    private ValidatableResponse getPaymentResponse(String paymentId, String strategy) {
        return given().port(app.getLocalPort())
                .header("X-Ledger", strategy)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get(PAYMENTS_PATH + paymentId)
                .then();
    }

    private ValidatableResponse getPaymentEventsResponse(String paymentId) {
        return getPaymentEventsResponse(paymentId, "");
    }

    private ValidatableResponse getPaymentEventsResponse(String paymentId, String strategy) {
        return given().port(app.getLocalPort())
                .header("X-Ledger", strategy)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get(String.format("/v1/payments/%s/events", paymentId))
                .then();
    }
}
