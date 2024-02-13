package uk.gov.pay.api.it;

import com.google.gson.GsonBuilder;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.it.fixtures.PaymentRefundJsonFixture;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.model.ledger.TransactionState;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;
import uk.gov.pay.api.utils.mocks.RefundTransactionFromLedgerFixture;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.validation.DateTimeUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.Urls.paymentLocationFor;
import static uk.gov.pay.api.utils.mocks.ChargeResponseFromConnector.ChargeResponseFromConnectorBuilder.aCreateOrGetChargeResponseFromConnector;
import static uk.gov.pay.api.utils.mocks.RefundTransactionFromLedgerFixture.RefundTransactionFromLedgerBuilder.aRefundTransactionFromLedgerFixture;
import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class PaymentRefundsResourceIT extends PaymentResourceITestBase {

    private static final int AMOUNT = 1000;
    private static final int REFUND_AMOUNT_AVAILABLE = 9000;
    private static final String CHARGE_ID = "ch_ab2341da231434l";
    private static final String REFUND_ID = "111999";
    private static final ZonedDateTime TIMESTAMP = DateTimeUtils.toUTCZonedDateTime("2016-01-01T12:00:00Z").get();
    private static final String CREATED_DATE = ISO_INSTANT_MILLISECOND_PRECISION.format(TIMESTAMP);
    private static final Address BILLING_ADDRESS = new Address("line1", "line2", "NR2 5 6EG", "city", "UK");
    private static final CardDetailsFromResponse CARD_DETAILS = new CardDetailsFromResponse("1234", "123456", "Mr. Payment", "12/19", BILLING_ADDRESS, "Visa", null);

    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);

    @Before
    public void setUp() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void getRefundByIdThroughConnector_shouldGetValidResponse() {
        connectorMockClient.respondWithGetRefundById(GATEWAY_ACCOUNT_ID, CHARGE_ID, REFUND_ID, AMOUNT, REFUND_AMOUNT_AVAILABLE, "available", CREATED_DATE);

        assertSingleRefund(getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID))
        .body("$", hasKey("settlement_summary"));
    }

    @Test
    public void getRefundByIdThroughLedger_shouldGetValidResponse() {
        ledgerMockClient.respondWithRefund(REFUND_ID, aRefundTransactionFromLedgerFixture()
                .withAmount((long) AMOUNT)
                .withState(new TransactionState("available", false))
                .withParentTransactionId(CHARGE_ID)
                .withTransactionId(REFUND_ID)
                .withCreatedDate(CREATED_DATE)
                .build());

        assertSingleRefund(getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID, "ledger-only"))
        .body("$", hasKey("settlement_summary"));
    }

    @Test
    public void getRefundByIdThroughLedger_shouldGetSettlementSummary() {
        ledgerMockClient.respondWithRefund(REFUND_ID, aRefundTransactionFromLedgerFixture()
                .withAmount((long) AMOUNT)
                .withState(new TransactionState("available", false))
                .withParentTransactionId(CHARGE_ID)
                .withTransactionId(REFUND_ID)
                .withCreatedDate(CREATED_DATE)
                .withSettlementSummary(ISO_INSTANT_MILLISECOND_PRECISION.format(TIMESTAMP))
                .build());

        ValidatableResponse response = getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID, "ledger-only");
        response.body("settlement_summary.settled_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(TIMESTAMP)));
    }

    private ValidatableResponse assertSingleRefund(ValidatableResponse paymentRefundByIdResponse) {
        return paymentRefundByIdResponse
                .statusCode(200)
                .contentType(JSON)
                .body("refund_id", is(REFUND_ID))
                .body("amount", is(AMOUNT))
                .body("status", is("available"))
                .body("created_date", is(CREATED_DATE))
                .body("_links.self.href", is(paymentRefundLocationFor(CHARGE_ID, REFUND_ID)))
                .body("_links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)))
                .body("settlement_summary", not(hasKey("settled_date")));
    }

    @Test
    public void getRefundById_shouldGetNonAuthorized_whenPublicAuthRespondsUnauthorised() {
        publicAuthMockClient.respondUnauthorised();

        getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID)
                .statusCode(401);
    }

    @Test
    public void getRefundByIdThroughConnector_shouldReturnNotFound_whenRefundDoesNotExist() {
        connectorMockClient.respondRefundNotFound(GATEWAY_ACCOUNT_ID, CHARGE_ID, "unknown-refund-id");

        getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID)
                .statusCode(404)
                .contentType(JSON)
                .body("code", is("P0700"))
                .body("description", is("Not found"));
    }

    @Test
    public void getRefundByIdThroughLedger_shouldReturnNotFound_whenRefundDoesNotExist() {
        ledgerMockClient.respondRefundNotFound("unknown-refund-id");

        getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID, "ledger-only")
                .statusCode(404)
                .contentType(JSON)
                .body("code", is("P0700"))
                .body("description", is("Not found"));
    }

    @Test
    public void getRefundById_returns500_whenConnectorRespondsWithResponseOtherThan200Or404() {
        connectorMockClient.respondRefundWithError(GATEWAY_ACCOUNT_ID, CHARGE_ID, REFUND_ID);

        getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID, CONNECTOR_ONLY_STRATEGY)
                .statusCode(500)
                .contentType(JSON)
                .body("code", is("P0798"))
                .body("description", is("Downstream system error"));
    }

    @Test
    public void getRefundById_returns500_whenLedgerRespondsWithResponseOtherThan200Or404() {
        ledgerMockClient.respondRefundWithError(REFUND_ID);

        getPaymentRefundByIdResponse(CHARGE_ID, REFUND_ID, "ledger-only")
                .statusCode(500)
                .contentType(JSON)
                .body("code", is("P0798"))
                .body("description", is("Downstream system error"));
    }

    @Test
    public void getRefundsThroughConnector_shouldGetValidResponse() {
        PaymentRefundJsonFixture refund1 = new PaymentRefundJsonFixture(100L, CREATED_DATE, "100", "available", new ArrayList<>());
        PaymentRefundJsonFixture refund2 = new PaymentRefundJsonFixture(300L, CREATED_DATE, "300", "pending", new ArrayList<>());

        connectorMockClient.respondWithGetAllRefunds(GATEWAY_ACCOUNT_ID, CHARGE_ID, refund1, refund2);

        assertRefundsResponse(getPaymentRefundsResponse(CHARGE_ID, CONNECTOR_ONLY_STRATEGY));
    }

    @Test
    public void getRefundsThroughLedger_shouldGetValidResponse() {
        RefundTransactionFromLedgerFixture refund1 = aRefundTransactionFromLedgerFixture()
                .withAmount(100L)
                .withCreatedDate(CREATED_DATE)
                .withTransactionId("100")
                .withState(new TransactionState("available", false))
                .build();

        RefundTransactionFromLedgerFixture refund2 = aRefundTransactionFromLedgerFixture()
                .withAmount(300L)
                .withCreatedDate(CREATED_DATE)
                .withTransactionId("300")
                .withState(new TransactionState("pending", false))
                .build();

        ledgerMockClient.respondWithGetAllRefunds(CHARGE_ID, refund1, refund2);

        assertRefundsResponse(getPaymentRefundsResponse(CHARGE_ID, "ledger-only"));
    }

    @Test
    public void getRefundsThroughLedger_shouldGetSettlementSummary() {
        RefundTransactionFromLedgerFixture refund1 = aRefundTransactionFromLedgerFixture()
                .withAmount(100L)
                .withCreatedDate(CREATED_DATE)
                .withTransactionId("100")
                .withState(new TransactionState("available", false))
                .withSettlementSummary(CREATED_DATE)
                .build();

        RefundTransactionFromLedgerFixture refund2 = aRefundTransactionFromLedgerFixture()
                .withAmount(300L)
                .withCreatedDate(CREATED_DATE)
                .withTransactionId("300")
                .withState(new TransactionState("pending", false))
                .build();

        ledgerMockClient.respondWithGetAllRefunds(CHARGE_ID, refund1, refund2);

        getPaymentRefundsResponse(CHARGE_ID, "ledger-only")
                .statusCode(200)
                .contentType(JSON)
                .body("_embedded.refunds[0].settlement_summary.settled_date", is(CREATED_DATE))
                .body("_embedded.refunds[1].settlement_summary", not(hasKey("settled_date")));


    }

    private void assertRefundsResponse(ValidatableResponse paymentRefundsResponse) {
        paymentRefundsResponse
                .statusCode(200)
                .contentType(JSON)
                .body("payment_id", is(CHARGE_ID))
                .body("_links.self.href", is(paymentRefundsLocationFor(CHARGE_ID)))
                .body("_links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)))
                .body("_embedded.refunds.size()", is(2))
                .body("_embedded.refunds[0].refund_id", is("100"))
                .body("_embedded.refunds[0].created_date", is(CREATED_DATE))
                .body("_embedded.refunds[0].amount", is(100))
                .body("_embedded.refunds[0].status", is("available"))
                .body("_embedded.refunds[0]._links.size()", is(2))
                .body("_embedded.refunds[0]._links.self.href", is(paymentRefundLocationFor(CHARGE_ID, "100")))
                .body("_embedded.refunds[0]._links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)))
                .body("_embedded.refunds[1].refund_id", is("300"))
                .body("_embedded.refunds[1].created_date", is(CREATED_DATE))
                .body("_embedded.refunds[1].amount", is(300))
                .body("_embedded.refunds[1].status", is("pending"))
                .body("_embedded.refunds[1]._links.size()", is(2))
                .body("_embedded.refunds[1]._links.self.href", is(paymentRefundLocationFor(CHARGE_ID, "300")))
                .body("_embedded.refunds[1]._links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)));
    }

    @Test
    public void getRefunds_shouldGetValidResponse_whenListReturnedIsEmpty() {
        connectorMockClient.respondWithGetAllRefunds(GATEWAY_ACCOUNT_ID, CHARGE_ID);

        assertEmptyRefundsResponse(getPaymentRefundsResponse(CHARGE_ID, CONNECTOR_ONLY_STRATEGY));
    }

    @Test
    public void getRefundsThroughLedger_shouldGetValidResponse_whenListReturnedIsEmpty() {
        ledgerMockClient.respondWithGetAllRefunds(CHARGE_ID);

        assertEmptyRefundsResponse(getPaymentRefundsResponse(CHARGE_ID, "ledger-only"));
    }

    private void assertEmptyRefundsResponse(ValidatableResponse paymentRefundsResponse) {
        paymentRefundsResponse
                .statusCode(200)
                .contentType(JSON)
                .body("payment_id", is(CHARGE_ID))
                .body("_links.self.href", is(paymentRefundsLocationFor(CHARGE_ID)))
                .body("_links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)))
                .body("_embedded.refunds.size()", is(0));
    }

    @Test
    public void getRefunds_shouldGetNonAuthorized_whenPublicAuthRespondsUnauthorised() {
        publicAuthMockClient.respondUnauthorised();

        getPaymentRefundsResponse(CHARGE_ID)
                .statusCode(401);
    }

    @Test
    public void createRefund_shouldGetAcceptedResponse() {
        String payload = new GsonBuilder().create().toJson(Map.of("amount", AMOUNT, "refund_amount_available", REFUND_AMOUNT_AVAILABLE));
        postRefundRequest(payload);
    }

    @Test
    public void createRefundWithNoRefundAmountAvailable_shouldGetAcceptedResponse() {
        String payload = new GsonBuilder().create().toJson(Map.of("amount", AMOUNT));

        connectorMockClient.respondWithChargeFound(null, GATEWAY_ACCOUNT_ID,
                aCreateOrGetChargeResponseFromConnector()
                        .withAmount(AMOUNT)
                        .withChargeId(CHARGE_ID)
                        .withLanguage(SupportedLanguage.ENGLISH)
                        .withDelayedCapture(false)
                        .withRefundSummary(new RefundSummary("available", 9000, 1000))
                        .withCardDetails(CARD_DETAILS)
                        .withGatewayTransactionId("gatewayTransactionId")
                        .build());

        postRefundRequest(payload);
    }

    @Test
    public void createRefundWhenChargeNotFound_shouldReturn404() {
        String payload = new GsonBuilder().create().toJson(Map.of("amount", AMOUNT));

        connectorMockClient.respondChargeNotFound(CHARGE_ID, GATEWAY_ACCOUNT_ID, "Not found");

        postRefunds(payload)
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void createRefundWhenRefundAmountAvailableMismatch_shouldReturn412Response() {
        String payload = new GsonBuilder().create().toJson(
                Map.of("amount", AMOUNT, "refund_amount_available", REFUND_AMOUNT_AVAILABLE));
        String errorMessage = new GsonBuilder().create().toJson(
                Map.of("code", "P0604", "description", "Refund amount available mismatch."));
        connectorMockClient.respondPreconditionFailed_whenCreateRefund(GATEWAY_ACCOUNT_ID, errorMessage, CHARGE_ID);

        postRefunds(payload)
                .then()
                .statusCode(PRECONDITION_FAILED.getStatusCode())
                .contentType(JSON)
                .body("code", is("P0604"))
                .body("description", is("Refund amount available mismatch."));
    }

    @Test
    public void createRefund_shouldGetNonAuthorized_whenPublicAuthRespondsUnauthorised() {
        publicAuthMockClient.respondUnauthorised();

        postRefunds("{\"amount\": 1000}")
                .then()
                .statusCode(401);
    }

    private void postRefundRequest(String payload) {
        String refundStatus = "available";
        connectorMockClient.respondAccepted_whenCreateARefund(AMOUNT, REFUND_AMOUNT_AVAILABLE, GATEWAY_ACCOUNT_ID, CHARGE_ID, REFUND_ID, refundStatus, CREATED_DATE);

        postRefunds(payload)
                .then()
                .statusCode(ACCEPTED.getStatusCode())
                .contentType(JSON)
                .body("refund_id", is(REFUND_ID))
                .body("amount", is(AMOUNT))
                .body("status", is(refundStatus))
                .body("created_date", is(CREATED_DATE))
                .body("_links.self.href", is(paymentRefundLocationFor(CHARGE_ID, REFUND_ID)))
                .body("_links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)));
    }

    private Response postRefunds(String payload) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .body(payload)
                .post(format("/v1/payments/%s/refunds", CHARGE_ID));
    }

    private ValidatableResponse getPaymentRefundByIdResponse(String paymentId, String refundId) {
        String defaultConnectorStrategy = "";
        return getPaymentRefundByIdResponse(paymentId, refundId, defaultConnectorStrategy);
    }

    private ValidatableResponse getPaymentRefundByIdResponse(String paymentId, String refundId, String strategy) {
        return given().port(app.getLocalPort())
                .header("X-Ledger", strategy)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get(format("/v1/payments/%s/refunds/%s", paymentId, refundId))
                .then();
    }

    private ValidatableResponse getPaymentRefundsResponse(String paymentId) {
        return getPaymentRefundsResponse(paymentId, "default");
    }

    private ValidatableResponse getPaymentRefundsResponse(String paymentId, String strategy) {
        return given().port(app.getLocalPort())
                .header("X-Ledger", strategy)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get(format("/v1/payments/%s/refunds", paymentId))
                .then();
    }

}
