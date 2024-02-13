package uk.gov.pay.api.it;

import org.apache.http.HttpStatus;
import org.junit.Test;
import uk.gov.pay.api.utils.PublicAuthMockClient;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static uk.gov.pay.api.utils.Payloads.aSuccessfulPaymentPayload;

public class AuthorisationIT extends PaymentResourceITestBase {
    
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Test
    public void shouldRefuseAuthorisationIfTokenIsNotPresent() {
        given().port(app.getLocalPort())
                .body(aSuccessfulPaymentPayload())
                .accept(JSON)
                .contentType(JSON)
                .post("/v1/payments/")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void shouldReturn500IfPublicAuthReturnsInvalidTokenPaymentType() {
        publicAuthMockClient.respondWithInvalidTokenType(API_KEY, "1");
        
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .body(aSuccessfulPaymentPayload())
                .accept(JSON)
                .contentType(JSON)
                .post("/v1/payments/")
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
}
