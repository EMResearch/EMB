package uk.gov.pay.api.utils.mocks;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import uk.gov.pay.api.it.fixtures.PaymentRefundJsonFixture;
import uk.gov.pay.api.it.fixtures.PaymentSingleResultBuilder;
import uk.gov.pay.api.model.Address;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.links.Link;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.utils.JsonStringBuilder;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.ErrorIdentifier;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.ACCEPTED_202;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.CONFLICT_409;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.eclipse.jetty.http.HttpStatus.PRECONDITION_FAILED_412;
import static org.eclipse.jetty.http.HttpStatus.UNPROCESSABLE_ENTITY_422;
import static uk.gov.pay.api.it.PaymentsResourceGetIT.AWAITING_CAPTURE_REQUEST;
import static uk.gov.pay.api.it.fixtures.PaymentSingleResultBuilder.aSuccessfulSinglePayment;
import static uk.gov.pay.api.utils.mocks.AgreementResponseFromConnector.AgreementResponseFromConnectorBuilder.aCreateAgreementResponseFromConnector;
import static uk.gov.pay.api.utils.mocks.ChargeResponseFromConnector.ChargeResponseFromConnectorBuilder.aCreateOrGetChargeResponseFromConnector;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ACCOUNT_NOT_LINKED_WITH_PSP;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AGREEMENT_NOT_ACTIVE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AGREEMENT_NOT_FOUND;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTHORISATION_API_NOT_ALLOWED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.CARD_NUMBER_IN_PAYMENT_LINK_REFERENCE_REJECTED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.GENERIC;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.IDEMPOTENCY_KEY_USED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.INCORRECT_AUTHORISATION_MODE_FOR_SAVE_PAYMENT_INSTRUMENT_TO_AGREEMENT;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.MOTO_NOT_ALLOWED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.REFUND_AMOUNT_AVAILABLE_MISMATCH;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.REFUND_NOT_AVAILABLE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.TELEPHONE_PAYMENT_NOTIFICATIONS_NOT_ALLOWED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ZERO_AMOUNT_NOT_ALLOWED;

public class ConnectorMockClient extends BaseConnectorMockClient {

    private static final String CONNECTOR_MOCK_CHARGE_EVENTS_PATH = CONNECTOR_MOCK_CHARGE_PATH + "/events";
    private static final String CONNECTOR_MOCK_CHARGE_REFUNDS_PATH = CONNECTOR_MOCK_CHARGE_PATH + "/refunds";
    private static final String CONNECTOR_MOCK_CHARGE_REFUND_BY_ID_PATH = CONNECTOR_MOCK_CHARGE_REFUNDS_PATH + "/%s";
    private static final DateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final String VALID_AGREEMENT_ID = "12345678901234567890123456";

    public ConnectorMockClient(WireMockClassRule connectorMock) {
        super(connectorMock);
    }

    private String buildChargeResponse(ChargeResponseFromConnector responseFromConnector) {
        PaymentSingleResultBuilder resultBuilder = aSuccessfulSinglePayment()
                .withChargeId(responseFromConnector.getChargeId())
                .withAmount(responseFromConnector.getAmount())
                .withMatchingReference(responseFromConnector.getReference())
                .withEmail(responseFromConnector.getEmail())
                .withDescription(responseFromConnector.getDescription())
                .withState(responseFromConnector.getState())
                .withReturnUrl(responseFromConnector.getReturnUrl())
                .withCreatedDate(responseFromConnector.getCreatedDate())
                .withLanguage(responseFromConnector.getLanguage())
                .withPaymentProvider(responseFromConnector.getPaymentProvider())
                .withDelayedCapture(responseFromConnector.isDelayedCapture())
                .withMoto(responseFromConnector.isMoto())
                .withAgreementId(responseFromConnector.getAgreementId())
                .withLinks(responseFromConnector.getLinks())
                .withSettlementSummary(responseFromConnector.getSettlementSummary())
                .withAuthorisationMode(responseFromConnector.getAuthorisationMode());
        
        ofNullable(responseFromConnector.getCardDetails()).ifPresent(resultBuilder::withCardDetails);
        ofNullable(responseFromConnector.getRefundSummary()).ifPresent(resultBuilder::withRefundSummary);
        ofNullable(responseFromConnector.getGatewayTransactionId()).ifPresent(resultBuilder::withGatewayTransactionId);
        ofNullable(responseFromConnector.getCorporateCardSurcharge()).ifPresent(resultBuilder::withCorporateCardSurcharge);
        ofNullable(responseFromConnector.getTotalAmount()).ifPresent(resultBuilder::withTotalAmount);
        ofNullable(responseFromConnector.getFee()).ifPresent(resultBuilder::withFee);
        ofNullable(responseFromConnector.getNetAmount()).ifPresent(resultBuilder::withNetAmount);
        ofNullable(responseFromConnector.getAuthorisationSummary()).ifPresent(resultBuilder::withAuthorisationSummary);
        ofNullable(responseFromConnector.getWalletType()).ifPresent(resultBuilder::withWalletType);
        responseFromConnector.getMetadata().ifPresent(resultBuilder::withMetadata);

        return resultBuilder.build();
    }

    private String buildAgreementResponse(AgreementResponseFromConnector responseFromConnector) {
        return new JsonStringBuilder()
                .add("reference", responseFromConnector.getReference())
                .add("agreement_id", responseFromConnector.getAgreementId())
                .add("service_id", responseFromConnector.getServiceId())
                .add("created_date", responseFromConnector.getCreatedDate())
                .add("live", responseFromConnector.isLive()).build();
    }

    private String buildTelephoneChargeResponse(ChargeResponseFromConnector responseFromConnector) {
        List<Map<String, Link>> links = new ArrayList<>();
        JsonStringBuilder request = new JsonStringBuilder()
                .add("amount", responseFromConnector.getAmount())
                .add("reference", responseFromConnector.getReference())
                .add("links", links)
                .add("description", responseFromConnector.getDescription())
                .add("processor_id", responseFromConnector.getProcessorId())
                .add("provider_id", responseFromConnector.getProviderId())
                .add("charge_id", responseFromConnector.getChargeId())
                .add("delayed_capture", false)
                .addToMap("state", "finished", responseFromConnector.getState().isFinished())
                .addToMap("state", "status", responseFromConnector.getState().getStatus())
                .addToMap("card_details", "card_brand", responseFromConnector.getCardDetails().getCardBrand())
                .addToMap("card_details", "expiry_date", responseFromConnector.getCardDetails().getExpiryDate())
                .addToMap("card_details", "last_digits_card_number", responseFromConnector.getCardDetails().getLastDigitsCardNumber())
                .addToMap("card_details", "first_digits_card_number", responseFromConnector.getCardDetails().getFirstDigitsCardNumber())
                .addToMap("payment_outcome", "status", responseFromConnector.getPaymentOutcome().getStatus());

        ofNullable(responseFromConnector.getAuthCode()).ifPresent(authCode -> request.add("auth_code", authCode));
        ofNullable(responseFromConnector.getCreatedDate()).ifPresent(createdDate -> request.add("created_date", createdDate));
        ofNullable(responseFromConnector.getAuthorisedDate()).ifPresent(authorisedDate -> request.add("authorised_date", authorisedDate));
        ofNullable(responseFromConnector.getEmail()).ifPresent(email -> request.add("email", email));
        ofNullable(responseFromConnector.getTelephoneNumber()).ifPresent(telephoneNumber -> request.add("telephone_number", telephoneNumber));
        ofNullable(responseFromConnector.getCardDetails().getCardHolderName()).ifPresent(cardHolderName -> request.addToMap("card_details", "cardholder_name", cardHolderName));

        return request.build();
    }

    private String buildGetRefundResponse(String refundId, int amount, int refundAmountAvailable, String status, String createdDate) {
        List<Map<String, Link>> links = new ArrayList<>();
        links.add(ImmutableMap.of("self", new Link("http://server:port/self-link")));
        links.add(ImmutableMap.of("payment", new Link("http://server:port/payment-link")));

        JsonStringBuilder jsonStringBuilder = new JsonStringBuilder()
                .add("refund_id", refundId)
                .add("amount", amount)
                .add("refund_amount_available", refundAmountAvailable)
                .add("status", status)
                .add("created_date", createdDate)
                .add("_links", links);

        return jsonStringBuilder.build();
    }

    private String buildChargeEventsResponse(String chargeId, List<Map<String, String>> events, ImmutableMap<?, ?>... links) {
        return new JsonStringBuilder()
                .add("charge_id", chargeId)
                .add("events", events)
                .add("links", asList(links))
                .build();
    }

    @Override
    String nextUrlPost() {
        return "http://frontend_card/charge/";
    }

    private String chargeEventsLocation(String accountId, String chargeId) {
        return format(CONNECTOR_MOCK_CHARGE_EVENTS_PATH, accountId, chargeId);
    }

    public void respondCreated_whenCreateTelephoneCharge(String gatewayAccountId, CreateTelephonePaymentRequest requestParams) {
        var responseFromConnector = aCreateOrGetChargeResponseFromConnector()
                .withAmount(requestParams.getAmount())
                .withDescription(requestParams.getDescription())
                .withReference(requestParams.getReference())
                .withProcessorId(requestParams.getProcessorId())
                .withProviderId(requestParams.getProviderId())
                .withCardDetails(new CardDetailsFromResponse(
                                requestParams.getLastFourDigits().orElse(null),
                                requestParams.getFirstSixDigits().orElse(null),
                                requestParams.getNameOnCard().orElse(null),
                                requestParams.getCardExpiry().orElse(null),
                                null,
                                requestParams.getCardType().orElse(null),
                                null
                        )
                )
                .withPaymentOutcome(requestParams.getPaymentOutcome())
                .withChargeId("dummypaymentid123notpersisted")
                .withDelayedCapture(false)
                .withState(new PaymentState("success", true, null, null));

        requestParams.getCreatedDate().ifPresent(responseFromConnector::withCreatedDate);
        requestParams.getAuthorisedDate().ifPresent(responseFromConnector::withAuthorisedDate);
        requestParams.getEmailAddress().ifPresent(responseFromConnector::withEmail);
        requestParams.getTelephoneNumber().ifPresent(responseFromConnector::withTelephoneNumber);
        requestParams.getAuthCode().ifPresent(responseFromConnector::withAuthCode);

        mockCreateTelephoneCharge(gatewayAccountId, aResponse()
                .withStatus(CREATED_201)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(buildTelephoneChargeResponse(responseFromConnector.buildTelephoneChargeResponse()))

        );
    }

    public void respondOk_whenCreateTelephoneCharge(String gatewayAccountId, CreateTelephonePaymentRequest requestParams) {
        var responseFromConnector = aCreateOrGetChargeResponseFromConnector()
                .withAmount(requestParams.getAmount())
                .withDescription(requestParams.getDescription())
                .withReference(requestParams.getReference())
                .withProcessorId(requestParams.getProcessorId())
                .withProviderId(requestParams.getProviderId())
                .withCardDetails(new CardDetailsFromResponse(
                                requestParams.getLastFourDigits().orElse(null),
                                requestParams.getFirstSixDigits().orElse(null),
                                requestParams.getNameOnCard().orElse(null),
                                requestParams.getCardExpiry().orElse(null),
                                null,
                                requestParams.getCardType().orElse(null),
                                null
                        )
                )
                .withPaymentOutcome(requestParams.getPaymentOutcome())
                .withChargeId("dummypaymentid123notpersisted")
                .withDelayedCapture(false)
                .withState(new PaymentState("success", true, null, null));

        requestParams.getCreatedDate().ifPresent(responseFromConnector::withCreatedDate);
        requestParams.getAuthorisedDate().ifPresent(responseFromConnector::withAuthorisedDate);
        requestParams.getEmailAddress().ifPresent(responseFromConnector::withEmail);
        requestParams.getTelephoneNumber().ifPresent(responseFromConnector::withTelephoneNumber);
        requestParams.getAuthCode().ifPresent(responseFromConnector::withAuthCode);

        mockCreateTelephoneCharge(gatewayAccountId, aResponse()
                .withStatus(OK_200)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(buildTelephoneChargeResponse(responseFromConnector.buildTelephoneChargeResponse()))
        );
    }

    public void respondCreated_whenCreateCharge(String gatewayAccountId, CreateChargeRequestParams requestParams) {

        var responseFromConnector = aCreateOrGetChargeResponseFromConnector()
                .withAmount(requestParams.getAmount())
                .withChargeId("chargeId")
                .withState(new PaymentState("created", false, null, null))
                .withReturnUrl(requestParams.getReturnUrl())
                .withDescription(requestParams.getDescription())
                .withReference(requestParams.getReference())
                .withPaymentProvider("Sandbox")
                .withGatewayTransactionId("gatewayTransactionId")
                .withCreatedDate(SDF.format(new Date()))
                .withLanguage(SupportedLanguage.ENGLISH)
                .withDelayedCapture(false)
                .withCardDetails(new CardDetailsFromResponse("1234", "123456", "Mr. Payment", "12/19", null, "Mastercard", "debit"))
                .withLink(validGetLink(chargeLocation(gatewayAccountId, "chargeId"), "self"))
                .withLink(validGetLink(nextUrl("chargeTokenId"), "next_url"))
                .withLink(validPostLink(nextUrlPost(), "next_url_post", "application/x-www-form-urlencoded", getChargeIdTokenMap("chargeTokenId", false)));

        requestParams.getSetUpAgreement().ifPresent(responseFromConnector::withAgreementId);

        if (!requestParams.getMetadata().isEmpty())
            responseFromConnector.withMetadata(requestParams.getMetadata());

        if (requestParams.getEmail() != null) {
            responseFromConnector.withEmail(requestParams.getEmail());
        }

        if (requestParams.getCardholderName().isPresent() || requestParams.getAddressLine1().isPresent() ||
                requestParams.getAddressLine2().isPresent() || requestParams.getAddressPostcode().isPresent() ||
                requestParams.getAddressCity().isPresent() || requestParams.getAddressCountry().isPresent()) {
            Address billingAddress = new Address(requestParams.getAddressLine1().orElse(null), requestParams.getAddressLine2().orElse(null),
                    requestParams.getAddressPostcode().orElse(null), requestParams.getAddressCity().orElse(null), requestParams.getAddressCountry().orElse(null));
            CardDetailsFromResponse cardDetails = new CardDetailsFromResponse(null, null, requestParams.getCardholderName().orElse(null),
                    null, billingAddress, null, null);
            responseFromConnector.withCardDetails(cardDetails);
        }

        mockCreateCharge(gatewayAccountId, aResponse()
                .withStatus(CREATED_201)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withHeader(LOCATION, chargeLocation(gatewayAccountId, "chargeId"))
                .withBody(buildChargeResponse(responseFromConnector.build())));
    }

    public void respondCreated_whenCreateCharge(String chargeTokenId, String gatewayAccountId, ChargeResponseFromConnector responseFromConnector) {
        ChargeResponseFromConnector build = aCreateOrGetChargeResponseFromConnector(responseFromConnector)
                .withLink(validGetLink(chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()), "self"))
                .withLink(validGetLink(nextUrl(chargeTokenId), "next_url"))
                .withLink(validPostLink(nextUrlPost(), "next_url_post", "application/x-www-form-urlencoded", getChargeIdTokenMap(chargeTokenId, false))).build();

        mockCreateCharge(gatewayAccountId,
                aResponse().withStatus(CREATED_201)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(LOCATION, chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()))
                        .withBody(buildChargeResponse(build)));
    }

    public void respondOK_whenCreateChargeIdempotencyKey(String chargeTokenId, String gatewayAccountId, ChargeResponseFromConnector responseFromConnector) {
        ChargeResponseFromConnector build = aCreateOrGetChargeResponseFromConnector(responseFromConnector)
                .withLink(validGetLink(chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()), "self"))
                .build();

        mockCreateCharge(gatewayAccountId,
                aResponse().withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(LOCATION, chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()))
                        .withBody(buildChargeResponse(build)));
    }

    public void respondCreated_whenCreateCharge_withAuthorisationMode_MotoApi(String chargeTokenId, String gatewayAccountId, ChargeResponseFromConnector responseFromConnector) {
        ChargeResponseFromConnector build = aCreateOrGetChargeResponseFromConnector(responseFromConnector)
                .withMoto(true)
                .withLink(validGetLink(chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()), "self"))
                .withLink(validPostLink(CONNECTOR_MOCK_AUTHORISATION_PATH, "auth_url_post", "application/json", getChargeIdTokenMap(chargeTokenId, true))).build();

        mockCreateCharge(gatewayAccountId,
                aResponse().withStatus(CREATED_201)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(LOCATION, chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()))
                        .withBody(buildChargeResponse(build)));
    }

    public void respondCreated_whenCreateCharge_withAuthorisationMode_Agreement(String chargeTokenId, String gatewayAccountId, ChargeResponseFromConnector responseFromConnector) {
        ChargeResponseFromConnector build = aCreateOrGetChargeResponseFromConnector(responseFromConnector)
                .withAuthorisationMode(AuthorisationMode.AGREEMENT)
                .withLink(validGetLink(chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()), "self"))
                .build();

        mockCreateCharge(gatewayAccountId,
                aResponse().withStatus(CREATED_201)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(LOCATION, chargeLocation(gatewayAccountId, responseFromConnector.getChargeId()))
                        .withBody(buildChargeResponse(build)));
    }

    public void respondCreated_whenCreateAgreement(String gatewayAccountId, CreateAgreementRequestParams requestParams) {
        var responseFromConnector = aCreateAgreementResponseFromConnector()
                .withReference(requestParams.getReference())
                .withAgreementId(VALID_AGREEMENT_ID)
                .withServiceId("service-id")
                .withCreatedDate("created-date")
                .withLive(true);
        mockCreateAgreement(gatewayAccountId, aResponse()
                .withStatus(CREATED_201)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(buildAgreementResponse(responseFromConnector.build())));
    }

    public void respondOk_whenCancelAgreement(String agreementId, String accountId) {
        whenCancelAgreement(agreementId, accountId, aResponse().withStatus(NO_CONTENT_204));
    }

    public void respondAgreementNotFound_WhenCancelAgreement(String agreementId, String accountId, String errorMsg) {
        respond_WhenCancelAgreement(agreementId, accountId, errorMsg, NOT_FOUND_404, AGREEMENT_NOT_FOUND);
    }

    public void respondAgreementNotActive_WhenCancelAgreement(String agreementId, String accountId, String errorMsg) {
        respond_WhenCancelAgreement(agreementId, accountId, errorMsg, BAD_REQUEST_400, AGREEMENT_NOT_ACTIVE);
    }

    public void respondError_WhenCancelAgreement(String agreementId, String accountId, String errorMsg) {
        respond_WhenCancelAgreement(agreementId, accountId, errorMsg, INTERNAL_SERVER_ERROR_500, GENERIC);
    }

    public void mockCreateTelephoneCharge(String gatewayAccountId, ResponseDefinitionBuilder responseDefinitionBuilder) {
        wireMockClassRule.stubFor(post(urlPathEqualTo(format(CONNECTOR_MOCK_TELEPHONE_CHARGES_PATH, gatewayAccountId)))
                .withHeader(CONTENT_TYPE, matching(APPLICATION_JSON)).willReturn(responseDefinitionBuilder));
    }

    public void respondAccepted_whenCreateARefund(int amount, int refundAmountAvailable, String gatewayAccountId, String chargeId, String refundId, String status, String createdDate) {
        whenCreateRefund(gatewayAccountId, chargeId, aResponse()
                .withStatus(ACCEPTED_202)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(buildGetRefundResponse(refundId, amount, refundAmountAvailable, status, createdDate)));
    }

    public void respondNotFound_whenCreateCharge(String gatewayAccountId) {
        mockCreateCharge(gatewayAccountId, aResponse().withStatus(NOT_FOUND_404));
    }

    public void respondBadRequest_whenCreateCharge(String gatewayAccountId, String errorMsg) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(BAD_REQUEST_400, errorMsg, GENERIC));
    }

    public void respondBadRequest_whenCreateAgreement(String gatewayAccountId, String errorMsg) {
        mockCreateAgreement(gatewayAccountId, withStatusAndErrorMessage(BAD_REQUEST_400, errorMsg, GENERIC));
    }

    public void respondAgreementNotFound_whenCreateCharge(String gatewayAccountId, String agreementId, String errorMsg) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(NOT_FOUND_404, format(errorMsg, agreementId), AGREEMENT_NOT_FOUND));
    }

    public void respondIncorrectAuthorisationModeForSavePaymentInstrumentToAgreement_whenCreateCharge(String gatewayAccountId, String agreementId, String errorMsg) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(BAD_REQUEST_400, format(errorMsg, agreementId), INCORRECT_AUTHORISATION_MODE_FOR_SAVE_PAYMENT_INSTRUMENT_TO_AGREEMENT));
    }

    public void respondPreconditionFailed_whenCreateRefund(String gatewayAccountId, String errorMsg, String chargeId) {
        whenCreateRefund(gatewayAccountId, chargeId, withStatusAndErrorMessage(PRECONDITION_FAILED_412, errorMsg, REFUND_AMOUNT_AVAILABLE_MISMATCH));
    }

    public void respondZeroAmountNotAllowed(String gatewayAccountId) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(UNPROCESSABLE_ENTITY_422, "anything", ZERO_AMOUNT_NOT_ALLOWED));
    }

    public void respondMotoPaymentNotAllowed(String gatewayAccountId) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(UNPROCESSABLE_ENTITY_422, "anything", MOTO_NOT_ALLOWED));
    }

    public void respondAuthorisationApiNotAllowed(String gatewayAccountId) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(UNPROCESSABLE_ENTITY_422, "anything", AUTHORISATION_API_NOT_ALLOWED));
    }

    public void respondTelephoneNotificationsNotEnabled(String gatewayAccountId) {
        mockCreateTelephoneCharge(gatewayAccountId, withStatusAndErrorMessage(FORBIDDEN_403, "anything", TELEPHONE_PAYMENT_NOTIFICATIONS_NOT_ALLOWED));
    }

    public void respondGatewayAccountCredentialNotConfigured(String gatewayAccountId) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(BAD_REQUEST_400, "Payment provider details are not configured on this account", ACCOUNT_NOT_LINKED_WITH_PSP));
    }

    public void respondCardNumberInReferenceError(String gatewayAccountId) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(BAD_REQUEST_400, "Card number entered in a payment link reference", CARD_NUMBER_IN_PAYMENT_LINK_REFERENCE_REJECTED));
    }

    public void respondWithChargeFound(String chargeTokenId, String gatewayAccountId, ChargeResponseFromConnector chargeResponseFromConnector) {
        respondWithChargeFound(chargeTokenId, gatewayAccountId, chargeResponseFromConnector, false);
    }

    public void respondWithChargeFound(String chargeTokenId, String gatewayAccountId, ChargeResponseFromConnector chargeResponseFromConnector, boolean isMotoApi) {
        String chargeResponseBody;
        String chargeId = chargeResponseFromConnector.getChargeId();

        var responseFromConnector = aCreateOrGetChargeResponseFromConnector(chargeResponseFromConnector)
                .withLink(validGetLink(chargeLocation(gatewayAccountId, chargeId), "self"))
                .withLink(validGetLink(chargeLocation(gatewayAccountId, chargeId) + "/refunds", "refunds"));

        if (AWAITING_CAPTURE_REQUEST == chargeResponseFromConnector.getState()) {
            responseFromConnector
                    .withLink(validPostLink(chargeLocation(gatewayAccountId, chargeId) + "/capture", "capture", "application/x-www-form-urlencoded", new HashMap<>()))
                    .build();
        } else {
            if (isMotoApi) {
                responseFromConnector
                        .withLink(validPostLink(CONNECTOR_MOCK_AUTHORISATION_PATH, "auth_url_post", "application/json", getChargeIdTokenMap(chargeTokenId, true)))
                        .build();
            } else {
                responseFromConnector
                        .withLink(validGetLink(nextUrl(chargeId), "next_url"))
                        .withLink(validPostLink(nextUrlPost(), "next_url_post", "application/x-www-form-urlencoded", getChargeIdTokenMap(chargeTokenId, false)))
                        .build();
            }
        }

        chargeResponseBody = buildChargeResponse(responseFromConnector.build());
        whenGetCharge(gatewayAccountId, chargeId, aResponse()
                .withStatus(OK_200)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(chargeResponseBody));
    }

    public void respondWithGetRefundById(String gatewayAccountId, String chargeId, String refundId, int amount, int totalRefundAmountAvailable, String refundStatus, String createdDate) {
        String refundResponse = buildGetRefundResponse(refundId, amount, totalRefundAmountAvailable, refundStatus, createdDate);
        whenGetRefundById(gatewayAccountId, chargeId, refundId, aResponse()
                .withStatus(OK_200)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(refundResponse));
    }

    public void respondWithGetAllRefunds(String gatewayAccountId, String chargeId, PaymentRefundJsonFixture... refunds) {

        Map<String, List<PaymentRefundJsonFixture>> refundList = new HashMap<>();
        refundList.put("refunds", Arrays.asList(refunds));

        List<Map<String, Link>> links = new ArrayList<>();
        links.add(ImmutableMap.of("self", new Link("http://server:port/self-link")));
        links.add(ImmutableMap.of("payment", new Link("http://server:port/payment-link")));

        JsonStringBuilder embedded = new JsonStringBuilder().noPrettyPrint();
        embedded.add("refunds", refundList);

        JsonStringBuilder jsonStringBuilder = new JsonStringBuilder()
                .add("payment_id", chargeId)
                .add("_links", links)
                .add("_embedded", refundList);

        whenGetAllRefunds(gatewayAccountId, chargeId, aResponse()
                .withStatus(OK_200)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(jsonStringBuilder.build()));
    }

    public void respondRefundNotFound(String gatewayAccountId, String chargeId, String refundId) {
        whenGetRefundById(gatewayAccountId, chargeId, refundId,
                withStatusAndErrorMessage(BAD_REQUEST_400, String.format("Refund with id [%s] not found.", refundId), GENERIC));

    }

    public void respondRefundWithError(String gatewayAccountId, String chargeId, String refundId) {
        whenGetRefundById(gatewayAccountId, chargeId, refundId,
                withStatusAndErrorMessage(INTERNAL_SERVER_ERROR_500, "server error", GENERIC));

    }

    public void respondWithChargeEventsFound(String gatewayAccountId, String chargeId, List<Map<String, String>> events) {
        whenGetChargeEvents(gatewayAccountId, chargeId, aResponse()
                .withStatus(OK_200)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(buildChargeEventsResponse(chargeId, events, validGetLink(chargeEventsLocation(gatewayAccountId, chargeId), "self"))));
    }


    public void respondChargeNotFound(String gatewayAccountId, String chargeId, String errorMsg) {
        respondWhenGetCharge(gatewayAccountId, chargeId, errorMsg, NOT_FOUND_404);
    }

    public void respondWhenGetCharge(String gatewayAccountId, String chargeId, String errorMsg, int status) {
        whenGetCharge(gatewayAccountId, chargeId, withStatusAndErrorMessage(status, errorMsg, GENERIC));
    }

    public void respondChargeEventsNotFound(String gatewayAccountId, String chargeId, String errorMsg) {
        respondWhenGetChargeEvents(gatewayAccountId, chargeId, errorMsg, NOT_FOUND_404);
    }

    public void respondWhenGetChargeEvents(String gatewayAccountId, String chargeId, String errorMsg, int status) {
        whenGetChargeEvents(gatewayAccountId, chargeId, withStatusAndErrorMessage(status, errorMsg, GENERIC));
    }

    public void respondOk_whenCancelCharge(String paymentId, String accountId) {
        whenCancelCharge(paymentId, accountId, aResponse().withStatus(NO_CONTENT_204));
    }

    public void respondOk_whenCaptureCharge(String paymentId, String accountId) {
        whenCaptureCharge(paymentId, accountId, aResponse().withStatus(NO_CONTENT_204));
    }

    public void respondChargeNotFound_WhenCancelCharge(String paymentId, String accountId, String errorMsg) {
        respond_WhenCancelCharge(paymentId, accountId, errorMsg, NOT_FOUND_404);
    }

    public void respondConflict_whenCreateChargeAndIdempotencyKeyUSed(String gatewayAccountId) {
        mockCreateCharge(gatewayAccountId, withStatusAndErrorMessage(CONFLICT_409,
                "The Idempotency-Key has already been used to create a payment", IDEMPOTENCY_KEY_USED, null));
    }

    public void respondChargeNotFound_WhenCaptureCharge(String paymentId, String accountId, String errorMsg) {
        respond_WhenCaptureCharge(paymentId, accountId, errorMsg, NOT_FOUND_404);
    }

    public void respondBadRequest_WhenCaptureCharge(String paymentId, String accountId, String errorMessage) {
        respond_WhenCaptureCharge(paymentId, accountId, errorMessage, BAD_REQUEST_400);
    }

    public void respond_WhenCancelCharge(String paymentId, String accountId, String errorMessage, int status) {
        whenCancelCharge(paymentId, accountId, withStatusAndErrorMessage(status, errorMessage, GENERIC, null));
    }

    public void respond_WhenCaptureCharge(String paymentId, String accountId, String errorMessage, int status) {
        whenCaptureCharge(paymentId, accountId, withStatusAndErrorMessage(status, errorMessage, GENERIC, null));
    }

    public void respond_WhenCancelAgreement(String paymentId, String accountId, String errorMessage, int status, ErrorIdentifier errorIdentifier) {
        whenCancelAgreement(paymentId, accountId, withStatusAndErrorMessage(status, errorMessage, errorIdentifier, null));
    }

    public void mockCreateCharge(String gatewayAccountId, ResponseDefinitionBuilder responseDefinitionBuilder) {
        wireMockClassRule.stubFor(post(urlPathEqualTo(format(CONNECTOR_MOCK_CHARGES_PATH, gatewayAccountId)))
                .withHeader(CONTENT_TYPE, matching(APPLICATION_JSON)).willReturn(responseDefinitionBuilder));
    }

    public void mockCreateAgreement(String gatewayAccountId, ResponseDefinitionBuilder responseDefinitionBuilder) {
        wireMockClassRule.stubFor(post(urlPathEqualTo(format(CONNECTOR_MOCK_AGREEMENTS_PATH, gatewayAccountId)))
                .withHeader(CONTENT_TYPE, matching(APPLICATION_JSON)).willReturn(responseDefinitionBuilder));
    }

    public void whenCancelAgreement(String agreementId, String gatewayAccountId, ResponseDefinitionBuilder responseDefinitionBuilder) {
        wireMockClassRule.stubFor(post(urlPathEqualTo(connectorCancelAgreementPathFor(agreementId, gatewayAccountId))).willReturn(responseDefinitionBuilder));
    }

    private void whenCreateRefund(String gatewayAccountId, String chargeId, ResponseDefinitionBuilder response) {
        wireMockClassRule.stubFor(post(urlPathEqualTo(format(CONNECTOR_MOCK_CHARGE_REFUNDS_PATH, gatewayAccountId, chargeId)))
                .willReturn(response));
    }

    private void whenGetRefundById(String gatewayAccountId, String chargeId, String refundId, ResponseDefinitionBuilder response) {
        wireMockClassRule.stubFor(get(urlPathEqualTo(format(CONNECTOR_MOCK_CHARGE_REFUND_BY_ID_PATH, gatewayAccountId, chargeId, refundId)))
                .willReturn(response));
    }

    private void whenGetAllRefunds(String gatewayAccountId, String chargeId, ResponseDefinitionBuilder response) {
        wireMockClassRule.stubFor(get(urlPathEqualTo(format(CONNECTOR_MOCK_CHARGE_REFUNDS_PATH, gatewayAccountId, chargeId)))
                .willReturn(response));
    }

    private void whenGetChargeEvents(String gatewayAccountId, String chargeId, ResponseDefinitionBuilder response) {
        wireMockClassRule.stubFor(get(urlPathEqualTo(format(CONNECTOR_MOCK_CHARGE_EVENTS_PATH, gatewayAccountId, chargeId)))
                .willReturn(response));
    }

    private void whenCancelCharge(String paymentId, String accountId, ResponseDefinitionBuilder response) {
        wireMockClassRule.stubFor(post(urlPathEqualTo(connectorCancelChargePathFor(paymentId, accountId))).willReturn(response));
    }

    private void whenCaptureCharge(String paymentId, String accountId, ResponseDefinitionBuilder response) {
        wireMockClassRule.stubFor(post(urlPathEqualTo(connectorCaptureChargePathFor(paymentId, accountId))).willReturn(response));
    }

    private String connectorCancelChargePathFor(String paymentId, String accountId) {
        return format(CONNECTOR_MOCK_CHARGE_PATH + "/cancel", accountId, paymentId);
    }

    private String connectorCaptureChargePathFor(String paymentId, String accountId) {
        return format(CONNECTOR_MOCK_CHARGE_PATH + "/capture", accountId, paymentId);
    }

    private String connectorCancelAgreementPathFor(String agreementId, String accountId) {
        return format(CONNECTOR_MOCK_AGREEMENT_PATH + "/cancel", accountId, agreementId);
    }

    private ResponseDefinitionBuilder withStatusAndErrorMessage(int statusCode, String errorMsg, ErrorIdentifier errorIdentifier) {
        return withStatusAndErrorMessage(statusCode, errorMsg, errorIdentifier, null);
    }

    private ResponseDefinitionBuilder withStatusAndErrorMessage(int statusCode, String errorMsg, ErrorIdentifier errorIdentifier, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", List.of(errorMsg));
        payload.put("error_identifier", errorIdentifier.toString());
        if (reason != null) {
            payload.put("reason", reason);
        }

        return aResponse()
                .withStatus(statusCode)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(new GsonBuilder().create().toJson(payload));
    }

    //"Gson can not automatically deserialize the pure inner classes since their no-args constructor"
    private Map<String, String> getChargeIdTokenMap(String chargeTokenId, boolean isMotoApi) {
        final Map<String, String> chargeTokenIdMap = new HashMap<>();
        chargeTokenIdMap.put(isMotoApi ? "one_time_token" : "chargeTokenId", chargeTokenId);
        return chargeTokenIdMap;
    }

    public void verifyCancelCharge(String paymentId, String accountId) {
        wireMockClassRule.verify(1, postRequestedFor(urlEqualTo(connectorCancelChargePathFor(paymentId, accountId))));
    }

    public void verifyCaptureCharge(String paymentId, String accountId) {
        wireMockClassRule.verify(1, postRequestedFor(urlEqualTo(connectorCaptureChargePathFor(paymentId, accountId))));
    }

    public void respondBadRequest_whenCreateARefund(String reason, String gatewayAccountId, String chargeId) {
        whenCreateRefund(gatewayAccountId, chargeId,
                withStatusAndErrorMessage(BAD_REQUEST_400,
                        "A message that should be completely ignored (only log)", REFUND_NOT_AVAILABLE, reason));
    }
}
