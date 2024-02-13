package uk.gov.pay.api.it;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.utils.JsonStringBuilder;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ChargeResponseFromConnector;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.pay.api.utils.mocks.CreateChargeRequestParams;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.validation.DateTimeUtils;

import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.mocks.ChargeResponseFromConnector.ChargeResponseFromConnectorBuilder.aCreateOrGetChargeResponseFromConnector;
import static uk.gov.pay.api.utils.mocks.CreateChargeRequestParams.CreateChargeRequestParamsBuilder.aCreateChargeRequestParams;
import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class PaymentsResourceCreateWithIdempotencyKeyIT extends PaymentResourceITestBase {

    private static final ZonedDateTime TIMESTAMP = DateTimeUtils.toUTCZonedDateTime("2016-01-01T12:00:00Z").get();
    private static final int AMOUNT = 9999999;
    private static final String CHARGE_ID = "ch_ab2341da231434l";
    private static final String CHARGE_TOKEN_ID = "token_1234567asdf";
    private static final PaymentState CREATED = new PaymentState("created", false, null, null);
    private static final RefundSummary REFUND_SUMMARY = new RefundSummary("pending", 100L, 50L);
    private static final String PAYMENT_PROVIDER = "Sandbox";
    private static final String CARD_BRAND_LABEL = "Mastercard";
    private static final String CARD_TYPE = "credit";
    private static final String RETURN_URL = "https://somewhere.gov.uk/rainbow/1";
    private static final String REFERENCE = "Some reference";
    private static final String DESCRIPTION = "Some description";
    private static final String CREATED_DATE = ISO_INSTANT_MILLISECOND_PRECISION.format(TIMESTAMP);
    private static final Address BILLING_ADDRESS = new Address("line1", "line2", "NR2 5 6EG", "city", "UK");
    private static final CardDetailsFromResponse CARD_DETAILS = new CardDetailsFromResponse("1234", "123456", "Mr. Payment", "12/19", BILLING_ADDRESS, CARD_BRAND_LABEL, CARD_TYPE);
    public static final String VALID_AGREEMENT_ID = "12345678901234567890123456";
    private static final String SUCCESS_PAYLOAD = paymentPayload(aCreateChargeRequestParams()
            .withAmount(AMOUNT)
            .withDescription(DESCRIPTION)
            .withReference(REFERENCE)
            .withReturnUrl(RETURN_URL).build());
    private static final String GATEWAY_TRANSACTION_ID = "gateway-tx-123456";
    private static final String IDEMPOTENCY_KEY = "an-idempotency-key";
    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Test
    public void createCardPaymentWithIdempotencyKeyReturns201() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);

        CreateChargeRequestParams createChargeRequestParams = aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription(DESCRIPTION)
                .withReference(REFERENCE)
                .withReturnUrl(RETURN_URL)
                .withAgreementId(VALID_AGREEMENT_ID)
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .build();
        connectorMockClient.respondCreated_whenCreateCharge(GATEWAY_ACCOUNT_ID, createChargeRequestParams);

        StringValuePattern idempotencyKeyMatcher = WireMock.equalTo(IDEMPOTENCY_KEY);

        postPaymentResponseWithIdempotencyKey(paymentPayload(createChargeRequestParams), IDEMPOTENCY_KEY)
                .statusCode(201)
                .contentType(JSON);

        connectorMockClient.verifyCreateChargeConnectorRequestWithHeader(GATEWAY_ACCOUNT_ID, paymentPayload(createChargeRequestParams), idempotencyKeyMatcher);
    }

    @Test
    public void createPaymentShouldReturn200_whenConnectorReturns200() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateChargeRequestParams createChargeRequestParams = aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription(DESCRIPTION)
                .withReference(REFERENCE)
                .withReturnUrl(RETURN_URL)
                .withAgreementId(VALID_AGREEMENT_ID)
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .build();
        connectorMockClient.respondOK_whenCreateChargeIdempotencyKey(CHARGE_TOKEN_ID, GATEWAY_ACCOUNT_ID, getConnectorCharge()
                .build());

        StringValuePattern idempotencyKeyMatcher = WireMock.equalTo(IDEMPOTENCY_KEY);

        postPaymentResponseWithIdempotencyKey(paymentPayload(createChargeRequestParams), IDEMPOTENCY_KEY)
                .statusCode(200)
                .contentType(JSON);

        connectorMockClient.verifyCreateChargeConnectorRequestWithHeader(GATEWAY_ACCOUNT_ID, paymentPayload(createChargeRequestParams), idempotencyKeyMatcher);
    }

    @Test
    public void createPaymentShouldReturn409_whenConnectorReturns409() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
        CreateChargeRequestParams createChargeRequestParams = aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription(DESCRIPTION)
                .withReference(REFERENCE)
                .withReturnUrl(RETURN_URL)
                .withAgreementId(VALID_AGREEMENT_ID)
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .build();
        connectorMockClient.respondConflict_whenCreateChargeAndIdempotencyKeyUSed(GATEWAY_ACCOUNT_ID);

        StringValuePattern idempotencyKeyMatcher = WireMock.equalTo(IDEMPOTENCY_KEY);

        postPaymentResponseWithIdempotencyKey(paymentPayload(createChargeRequestParams), IDEMPOTENCY_KEY)
                .statusCode(409)
                .contentType(JSON)
                .body("code", is("P0191"))
                .body("description", is("The `Idempotency-Key` you sent in the request header has already been used to create a payment."));

        connectorMockClient.verifyCreateChargeConnectorRequestWithHeader(GATEWAY_ACCOUNT_ID, paymentPayload(createChargeRequestParams), idempotencyKeyMatcher);
    }

    @Test
    public void createPayment_responseWith422_whenIdempotencyKeyIsEmpty() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);

        postPaymentResponseWithIdempotencyKey(SUCCESS_PAYLOAD, "")
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0102"))
                .body("header", is("Idempotency-Key"))
                .body("description", is("Header [Idempotency-Key] can have a size between 1 and 255"));
    }

    @Test
    public void createPayment_responseWith422_whenIdempotencyKeyIsTooLong() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);

        postPaymentResponseWithIdempotencyKey(SUCCESS_PAYLOAD, RandomStringUtils.randomAlphanumeric(256))
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0102"))
                .body("header", is("Idempotency-Key"))
                .body("description", is("Header [Idempotency-Key] can have a size between 1 and 255"));
    }

    @Test
    public void createPayment_responseWith422_whenIdempotencyKeyContainsSpecialCharacters() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);

        postPaymentResponseWithIdempotencyKey(SUCCESS_PAYLOAD, "123$@!?")
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0102"))
                .body("header", is("Idempotency-Key"))
                .body("description", is("Header [Idempotency-Key] can only contain alphanumeric characters and hyphens"));
    }


    private static String paymentPayload(CreateChargeRequestParams params) {
        JsonStringBuilder payload = new JsonStringBuilder()
                .add("amount", params.getAmount())
                .add("reference", params.getReference())
                .add("description", params.getDescription())
                .add("return_url", params.getReturnUrl());

        if (!params.getMetadata().isEmpty()) {
            payload.add("metadata", params.getMetadata());
        }

        params.getSource().ifPresent(source -> payload.addToNestedMap("source", source, "internal"));
        params.getSetUpAgreement().ifPresent(setUpAgreement -> payload.add("set_up_agreement", setUpAgreement));
        params.getAgreementId().ifPresent(agreementId -> payload.add("agreement_id", agreementId));
        params.getAuthorisationMode().ifPresent(authorisationMode -> payload.add("authorisation_mode", authorisationMode));

        return payload.build();
    }

    private ChargeResponseFromConnector.ChargeResponseFromConnectorBuilder getConnectorCharge() {
        return aCreateOrGetChargeResponseFromConnector()
                .withAmount(AMOUNT)
                .withChargeId(CHARGE_ID)
                .withState(CREATED)
                .withReturnUrl(RETURN_URL)
                .withDescription(DESCRIPTION)
                .withReference(REFERENCE)
                .withPaymentProvider(PAYMENT_PROVIDER)
                .withCreatedDate(CREATED_DATE)
                .withLanguage(SupportedLanguage.ENGLISH)
                .withDelayedCapture(true)
                .withMoto(false)
                .withRefundSummary(REFUND_SUMMARY)
                .withCardDetails(CARD_DETAILS)
                .withGatewayTransactionId(GATEWAY_TRANSACTION_ID);
    }

    private ValidatableResponse postPaymentResponseWithIdempotencyKey(String payload, String idempotencyKey) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .header("Idempotency-Key", idempotencyKey)
                .post(PAYMENTS_PATH)
                .then();
    }
}
