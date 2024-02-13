package uk.gov.pay.api.it;

import com.jayway.jsonassert.JsonAssert;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;

import java.io.IOException;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.jetty.http.HttpStatus.CONFLICT_409;
import static org.eclipse.jetty.http.HttpStatus.LENGTH_REQUIRED_411;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class PaymentsResourceCaptureIT extends PaymentResourceITestBase {

    private static final String TEST_CHARGE_ID = "ch_e36c168c41a0";
    private static final String CAPTURE_PAYMENTS_PATH = PAYMENTS_PATH + "%s/capture";

    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Test
    public void capturePayment_Returns401_WhenUnauthorised() {
        publicAuthMockClient.respondUnauthorised();
        postCapturePaymentResponse(TEST_CHARGE_ID).statusCode(401);
    }

    @Test
    public void successful_whenConnector_AllowsCapture() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respondOk_whenCaptureCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID);
        postCapturePaymentResponse(TEST_CHARGE_ID).statusCode(204);
        connectorMockClient.verifyCaptureCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void capturePayment_returns400_whenConnectorRespondsWithA400() throws IOException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respondBadRequest_WhenCaptureCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID, "Invalid account Id");

        InputStream body = postCapturePaymentResponse(TEST_CHARGE_ID)
                .statusCode(400)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P1001"))
                .assertThat("$.description", is("Capture of payment failed"));
    }

    @Test
    public void capturePayment_returns404_whenPaymentNotFound() throws IOException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respondChargeNotFound_WhenCaptureCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID, "some backend error message");

        InputStream body = postCapturePaymentResponse(TEST_CHARGE_ID)
                .statusCode(404)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P1000"))
                .assertThat("$.description", is("Not found"));
    }

    @Test
    public void capturePayment_returns409_whenConnectorReturns409() throws IOException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respond_WhenCaptureCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID, "some backend error message", CONFLICT_409);

        InputStream body = postCapturePaymentResponse(TEST_CHARGE_ID)
                .statusCode(409)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P1003"))
                .assertThat("$.description", is("Payment cannot be captured"));
    }

    @Test
    public void capturePayment_returns500_whenConnectorResponseIsUnexpected() throws IOException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respond_WhenCaptureCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID, "some backend error message", LENGTH_REQUIRED_411);

        InputStream body = postCapturePaymentResponse(TEST_CHARGE_ID)
                .statusCode(500)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P1098"))
                .assertThat("$.description", is("Downstream system error"));
    }

    private ValidatableResponse postCapturePaymentResponse(String paymentId) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(String.format(CAPTURE_PAYMENTS_PATH, paymentId))
                .then();
    }

}
