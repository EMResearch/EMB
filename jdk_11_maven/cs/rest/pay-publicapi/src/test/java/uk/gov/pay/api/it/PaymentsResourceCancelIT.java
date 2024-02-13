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

public class PaymentsResourceCancelIT extends PaymentResourceITestBase {

    private static final String TEST_CHARGE_ID = "ch_ab2341da231434";
    private static final String CANCEL_PAYMENTS_PATH = PAYMENTS_PATH + "%s/cancel";

    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    
    @Test
    public void cancelPayment_Returns401_WhenUnauthorised() {
        publicAuthMockClient.respondUnauthorised();
        postCancelPaymentResponse(TEST_CHARGE_ID).statusCode(401);
    }

    @Test
    public void successful_whenConnector_AllowsCancellation() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respondOk_whenCancelCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID);
        postCancelPaymentResponse(TEST_CHARGE_ID).statusCode(204);
        connectorMockClient.verifyCancelCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void cancelPayment_returns404_whenPaymentNotFound() throws IOException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respondChargeNotFound_WhenCancelCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID, "some backend error message");

        InputStream body = postCancelPaymentResponse(TEST_CHARGE_ID)
                .statusCode(404)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0500"))
                .assertThat("$.description", is("Not found"));
    }

    @Test
    public void cancelPayment_returns409_whenConnectorReturns409() throws IOException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respond_WhenCancelCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID, "some backend error message", CONFLICT_409);

        InputStream body = postCancelPaymentResponse(TEST_CHARGE_ID)
                .statusCode(409)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0502"))
                .assertThat("$.description", is("Cancellation of payment failed"));
    }

    @Test
    public void cancelPayment_returns500_whenConnectorResponseIsUnexpected() throws IOException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        connectorMockClient.respond_WhenCancelCharge(TEST_CHARGE_ID, GATEWAY_ACCOUNT_ID, "some backend error message", LENGTH_REQUIRED_411);

        InputStream body = postCancelPaymentResponse(TEST_CHARGE_ID)
                .statusCode(500)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0598"))
                .assertThat("$.description", is("Downstream system error"));
    }

    private ValidatableResponse postCancelPaymentResponse(String paymentId) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(String.format(CANCEL_PAYMENTS_PATH, paymentId))
                .then();
    }
}
