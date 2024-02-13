package uk.gov.pay.api.it.validation;

import com.jayway.jsonassert.JsonAssert;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.it.PaymentResourceITestBase;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.io.IOException;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.mocks.ChargeResponseFromConnector.ChargeResponseFromConnectorBuilder.aCreateOrGetChargeResponseFromConnector;

public class PaymentsRefundsResourceAmountValidationIT extends PaymentResourceITestBase {

    private static final int REFUND_AMOUNT_AVAILABLE = 9000;
    private static final Address BILLING_ADDRESS = new Address("line1", "line2", "NR2 5 6EG", "city", "UK");
    private static final CardDetailsFromResponse CARD_DETAILS = new CardDetailsFromResponse("1234", "123456", "Mr. Payment", "12/19", BILLING_ADDRESS, "Visa", "credit");

    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    
    @Before
    public void setUpBearerToken() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void createPaymentRefund_responseWith422_whenAmountIsNegative() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": -123\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(422)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0602"))
                .assertThat("$.description", is("Invalid attribute value: amount. Must be greater than or equal to 1"));
    }

    @Test
    public void createPaymentRefund_responseWith422_whenAmountIsBiggerThanTheMaximumAllowed() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 10000001\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(422)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0602"))
                .assertThat("$.description", is("Invalid attribute value: amount. Must be less than or equal to 10000000"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountFieldHasNullValue() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": null\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0601"))
                .assertThat("$.description", is("Missing mandatory attribute: amount"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountFieldIsNotNumeric() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": \"hola world!\"\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0602"))
                .assertThat("$.description", is("Invalid attribute value: amount. Must be a valid numeric format"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountFieldIsNotAValidJsonField() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": {\n" +
                "    \"whatever\": 1\n" +
                "  }\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0602"))
                .assertThat("$.description", is("Invalid attribute value: amount. Must be a valid numeric format"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountFieldIsBlank() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": \"    \"\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0602"))
                .assertThat("$.description", is("Invalid attribute value: amount. Must be a valid numeric format"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountFieldIsMissing() throws IOException {
        // language=JSON
        String payload = "{}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0601"))
                .assertThat("$.description", is("Missing mandatory attribute: amount"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountIsHexadecimal() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 0x1000\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0697"))
                .assertThat("$.description", is("Unable to parse JSON"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountIsBinary() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 0B101\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0697"))
                .assertThat("$.description", is("Unable to parse JSON"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountIsOctal() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 017\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0697"))
                .assertThat("$.description", is("Unable to parse JSON"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountIsNullByteEncoded() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": %00\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0697"))
                .assertThat("$.description", is("Unable to parse JSON"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenAmountIsFloat() throws IOException {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 27.55\n" +
                "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, "chargeId", payload)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(3))
                .assertThat("$.field", is("amount"))
                .assertThat("$.code", is("P0602"))
                .assertThat("$.description", is("Invalid attribute value: amount. Must be a valid numeric format"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenConnectorResponseIsErrorDueToAmountRequestedIsNotAvailableForRefund() throws IOException {
        int amount = 1000;
        String externalChargeId = "charge_12345";

        connectorMockClient.respondWithChargeFound(null, GATEWAY_ACCOUNT_ID,
                aCreateOrGetChargeResponseFromConnector()
                        .withAmount(amount)
                        .withChargeId(externalChargeId)
                        .withLanguage(SupportedLanguage.ENGLISH)
                        .withDelayedCapture(false)
                        .withRefundSummary(new RefundSummary("available", REFUND_AMOUNT_AVAILABLE, 1000))
                        .withCardDetails(CARD_DETAILS)
                        .withGatewayTransactionId("gatewayTransactionId")
                        .build());
        
        connectorMockClient.respondBadRequest_whenCreateARefund("full", GATEWAY_ACCOUNT_ID, externalChargeId);

        String refundRequest = "{\"amount\":" + amount + "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, externalChargeId, refundRequest)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0603"))
                .assertThat("$.description", is("The payment is not available for refund. Payment refund status: full"));
    }

    @Test
    public void createPaymentRefund_responseWith400_whenConnectorResponseIsErrorDueToChargeStatusMakesPaymentNonRefundable() throws IOException {
        int amount = 1000;
        String externalChargeId = "charge_12345";

        connectorMockClient.respondWithChargeFound(null, GATEWAY_ACCOUNT_ID,
                aCreateOrGetChargeResponseFromConnector()
                        .withAmount(amount)
                        .withChargeId(externalChargeId)
                        .withLanguage(SupportedLanguage.ENGLISH)
                        .withDelayedCapture(false)
                        .withRefundSummary(new RefundSummary("available", REFUND_AMOUNT_AVAILABLE, 1000))
                        .withCardDetails(CARD_DETAILS)
                        .withGatewayTransactionId("gatewayTransactionId")
                        .build());
        
        connectorMockClient.respondBadRequest_whenCreateARefund("pending", GATEWAY_ACCOUNT_ID, externalChargeId);

        String refundRequest = "{\"amount\":" + amount + "}";

        InputStream body = postPaymentRefundAndThen(API_KEY, externalChargeId, refundRequest)
                .statusCode(400)
                .contentType(JSON)
                .extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0603"))
                .assertThat("$.description", is("The payment is not available for refund. Payment refund status: pending"));
    }

    private ValidatableResponse postPaymentRefundAndThen(String bearerToken, String chargeId, String payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .post(PAYMENTS_PATH + chargeId + "/refunds")
                .then();
    }
}
