package uk.gov.pay.api.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.model.CreateAgreementRequestBuilder;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.pay.api.utils.mocks.CreateAgreementRequestParams;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.Payloads.agreementPayload;
import static uk.gov.pay.api.utils.mocks.AgreementFromLedgerFixture.AgreementFromLedgerFixtureBuilder.anAgreementFromLedgerWithoutPaymentInstrumentFixture;
import static uk.gov.pay.api.utils.mocks.CreateAgreementRequestParams.CreateAgreementRequestParamsBuilder.aCreateAgreementRequestParams;

public class AgreementsApiResourceCreateIT extends PaymentResourceITestBase {
   
    private static final String REFERENCE = "Some reference <script> alert('This is a ?{simple} XSS attack.')</script>";
    private static final String DESCRIPTION = "A valid description";
    private static final String USER_IDENTIFIER = "a-valid-user-identifier";
    public static final String VALID_AGREEMENT_ID = "12345678901234567890123456";
    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);

    @Test
    public void shouldCreateAgreement() throws JsonProcessingException {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference(REFERENCE)
                .withDescription(DESCRIPTION)
                .withUserIdentifier(USER_IDENTIFIER)
                .build();
        var agreementFixture = anAgreementFromLedgerWithoutPaymentInstrumentFixture()
                .withExternalId(VALID_AGREEMENT_ID)
                .withPaymentInstrument(null)
                .build();
        connectorMockClient.respondCreated_whenCreateAgreement(GATEWAY_ACCOUNT_ID, createAgreementRequestParams);
        ledgerMockClient.respondWithAgreement(VALID_AGREEMENT_ID, agreementFixture);
        postAgreementRequest(agreementPayload(createAgreementRequestParams))
                .statusCode(HttpStatus.SC_CREATED)
                .contentType(JSON)
                .body("agreement_id", is(VALID_AGREEMENT_ID))
                .body("reference", is("valid-reference"))
                .body("description", is("An agreement description"));
        connectorMockClient.verifyCreateAgreementConnectorRequest(GATEWAY_ACCOUNT_ID, createAgreementRequestParams);
    }

    @Test
    public void shouldReturn400WhenWhenReferenceIsEmptyString() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference("")
                .build();
        postAgreementRequest(agreementPayload(createAgreementRequestParams))
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(JSON)
                .body("field", is("reference"))
                .body("code", is("P2101"))
                .body("description", is("Missing mandatory attribute: reference"));
    }

    @Test
    public void shouldReturn400WhenWhenReferenceIsNull() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequest agreementRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder.builder().reference(null).description(DESCRIPTION));
        postAgreementRequest(agreementRequest.toConnectorPayload())
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(JSON)
                .body("field", is("reference"))
                .body("code", is("P2101"))
                .body("description", is("Missing mandatory attribute: reference"));
    }

    @Test
    public void shouldReturn400WhenWhenDescriptionIsNull() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequest agreementRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder.builder().reference(REFERENCE).description(null));
        postAgreementRequest(agreementRequest.toConnectorPayload())
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(JSON)
                .body("field", is("description"))
                .body("code", is("P2101"))
                .body("description", is("Missing mandatory attribute: description"));
    }

    @Test
    public void shouldReturn400WhenWhenDescriptionIsEmptyString() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequest agreementRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder.builder().reference(REFERENCE).description(""));
        postAgreementRequest(agreementRequest.toConnectorPayload())
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(JSON)
                .body("field", is("description"))
                .body("code", is("P2101"))
                .body("description", is("Missing mandatory attribute: description"));
    }

    @Test
    public void shouldReturn422WhenWhenDescriptionIsTooLong() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequest agreementRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder.builder().reference(REFERENCE).description(random(256, true, true)));
        postAgreementRequest(agreementRequest.toConnectorPayload())
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(JSON)
                .body("field", is("description"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: description. Must be less than or equal to 255 characters length"));
    }

    @Test
    public void shouldReturn201WhenWhenOptionalUserIdentifierIsNull() throws JsonProcessingException {
        var agreementFixture = anAgreementFromLedgerWithoutPaymentInstrumentFixture()
                .withExternalId(VALID_AGREEMENT_ID)
                .withPaymentInstrument(null)
                .build();
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);

        var params = aCreateAgreementRequestParams()
                .withReference(REFERENCE)
                .withDescription(DESCRIPTION)
                .withUserIdentifier(null)
                .build();
        connectorMockClient.respondCreated_whenCreateAgreement(GATEWAY_ACCOUNT_ID, params);
        ledgerMockClient.respondWithAgreement(VALID_AGREEMENT_ID, agreementFixture);
        postAgreementRequest(agreementPayload(params))
                .statusCode(HttpStatus.SC_CREATED)
                .contentType(JSON);
    }

    @Test
    public void shouldReturn422WhenWhenUserIdentifierIsEmptyString() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequest agreementRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder.builder().reference(REFERENCE).description(DESCRIPTION).userIdentifier(""));
        postAgreementRequest(agreementRequest.toConnectorPayload())
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(JSON)
                .body("field", is("user_identifier"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: user_identifier. Must be less than or equal to 255 characters length"));
    }

    @Test
    public void shouldReturn422WhenWhenUserIdentifierIsTooLong() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateAgreementRequest agreementRequest = new CreateAgreementRequest(CreateAgreementRequestBuilder.builder().reference(REFERENCE).description(DESCRIPTION).userIdentifier(random(256, true, true)));
        postAgreementRequest(agreementRequest.toConnectorPayload())
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(JSON)
                .body("field", is("user_identifier"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: user_identifier. Must be less than or equal to 255 characters length"));
    }

    @Test
    public void shouldReturn422WhenWhenReferenceIsTooLong() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        String tooLongReference = random(256, true, true);
        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference(tooLongReference)
                .withDescription(DESCRIPTION)
                .build();
        
        postAgreementRequest(agreementPayload(createAgreementRequestParams))
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(JSON)
                .body("field", is("reference"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: reference. Must be less than or equal to 255 characters length"));
    }
 
    @Test
  public void createPayment_responseWith500_whenConnectorResponseIsAnUnrecognisedError() throws Exception {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);

        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference(REFERENCE)
                .withDescription(DESCRIPTION)
                .build();
        connectorMockClient.respondBadRequest_whenCreateAgreement(GATEWAY_ACCOUNT_ID, "Downstream system error");

       postAgreementRequest(agreementPayload(createAgreementRequestParams))
               .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .contentType(JSON)
               .body("code",is("P2198"))
               .body("description", is("Downstream system error"));
      
        connectorMockClient.verifyCreateAgreementConnectorRequest(GATEWAY_ACCOUNT_ID, createAgreementRequestParams);
    }

    @Test
    public void createAgreement_Returns401_WhenUnauthorised() {
        publicAuthMockClient.respondUnauthorised();
        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference(REFERENCE)
                .withDescription(DESCRIPTION)
                .build();
        postAgreementRequest(agreementPayload(createAgreementRequestParams)).statusCode(401);
    }
    
    @Test
    public void createAgreement_Returns_WhenPublicAuthInaccessible() {
        publicAuthMockClient.respondWithError();
        CreateAgreementRequestParams createAgreementRequestParams = aCreateAgreementRequestParams()
                .withReference(REFERENCE)
                .build();
        postAgreementRequest(agreementPayload(createAgreementRequestParams)).statusCode(503);
    }

    protected ValidatableResponse postAgreementRequest(String payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .post("/v1/agreements")
                .then();
    }
}
