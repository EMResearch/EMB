package uk.gov.pay.api.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.mocks.AgreementFromLedgerFixture.AgreementFromLedgerFixtureBuilder.anAgreementFromLedgerWithoutPaymentInstrumentFixture;

public class AgreementsApiResourceCancelIT extends PaymentResourceITestBase {

    private final PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private final LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);
    private final ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);

    private final String agreementId = "an-agreement-id";

    @Before
    public void setUp() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void getAgreementFromLedger() throws JsonProcessingException {
        var fixture = anAgreementFromLedgerWithoutPaymentInstrumentFixture()
                .withExternalId(agreementId)
                .build();
        ledgerMockClient.respondWithAgreement(agreementId, fixture);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get(AGREEMENTS_PATH + agreementId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("agreement_id", is(agreementId))
                .body("$", not(hasKey("service_id")))
                .body("reference", is(fixture.getReference()))
                .body("description", is(fixture.getDescription()))
                .body("status", is(fixture.getStatus().toLowerCase()))
                .body("created_date", is(fixture.getCreatedDate()))
                .body("$", not(hasKey("user_identifier")))
                .body("$", not(hasKey("cancelled_date")))
                .body("$", not(hasKey("cancelled_by_user_email")));
    }

    @Test
    public void getCancelledAgreementFromLedger() throws JsonProcessingException {
        var fixture = anAgreementFromLedgerWithoutPaymentInstrumentFixture()
                .withExternalId(agreementId)
                .withStatus("CANCELLED")
                .withServiceId("service-id")
                .withCancelledDate("2023-06-14T12:40:00.000Z")
                .build();
        ledgerMockClient.respondWithAgreement(agreementId, fixture);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get(AGREEMENTS_PATH + agreementId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("agreement_id", is(agreementId))
                .body("$", not(hasKey("service_id")))
                .body("reference", is(fixture.getReference()))
                .body("description", is(fixture.getDescription()))
                .body("status", is(fixture.getStatus().toLowerCase()))
                .body("created_date", is(fixture.getCreatedDate()))
                .body("$", not(hasKey("user_identifier")))
                .body("cancelled_date", is(fixture.getCancelledDate()));
    }

    @Test
    public void getAgreement_MissingAgreementShouldMapException() {
        ledgerMockClient.respondAgreementNotFound(agreementId);
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get(AGREEMENTS_PATH + agreementId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", is("P2200"))
                .body("description", is("Not found"));
    }
    
    @Test
    public void cancelAgreement() {
        connectorMockClient.respondOk_whenCancelAgreement(agreementId, GATEWAY_ACCOUNT_ID);
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .post(AGREEMENTS_PATH + agreementId + "/cancel")
                .then()
                .statusCode(204);
    }

    @Test
    public void cancelAgreementReturnsErrorWhenConnectorRespondsAgreementNotFound() {
        connectorMockClient.respondAgreementNotFound_WhenCancelAgreement(agreementId, GATEWAY_ACCOUNT_ID, "error message");
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .post(AGREEMENTS_PATH + agreementId + "/cancel")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", is("P2500"))
                .body("description", is("Not found"));
    }

    @Test
    public void cancelAgreementReturnsErrorWhenConnectorRespondsAgreementNotActive() {
        connectorMockClient.respondAgreementNotActive_WhenCancelAgreement(agreementId, GATEWAY_ACCOUNT_ID, "error message");
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .post(AGREEMENTS_PATH + agreementId + "/cancel")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", is("P2501"))
                .body("description", is("Cancellation of agreement failed"));
    }

    @Test
    public void cancelAgreementReturnsErrorWhenConnectorRespondsWithError() {
        connectorMockClient.respondError_WhenCancelAgreement(agreementId, GATEWAY_ACCOUNT_ID, "error message");
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .post(AGREEMENTS_PATH + agreementId + "/cancel")
                .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("code", is("P2598"))
                .body("description", is("Downstream system error"));
    }

}
