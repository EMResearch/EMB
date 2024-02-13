package uk.gov.pay.api.utils.mocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import uk.gov.pay.api.utils.JsonStringBuilder;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static uk.gov.service.payments.commons.model.Source.CARD_API;

public abstract class BaseConnectorMockClient {

    static String CONNECTOR_MOCK_ACCOUNTS_PATH = "/v1/api/accounts/%s";
    static String CONNECTOR_MOCK_CHARGES_PATH = CONNECTOR_MOCK_ACCOUNTS_PATH + "/charges";
    static String CONNECTOR_MOCK_TELEPHONE_CHARGES_PATH = CONNECTOR_MOCK_ACCOUNTS_PATH + "/telephone-charges";
    static String CONNECTOR_MOCK_CHARGE_PATH = CONNECTOR_MOCK_CHARGES_PATH + "/%s";
    static String CONNECTOR_MOCK_AGREEMENTS_PATH = CONNECTOR_MOCK_ACCOUNTS_PATH + "/agreements";
    static String CONNECTOR_MOCK_AGREEMENT_PATH = CONNECTOR_MOCK_AGREEMENTS_PATH + "/%s";
    static String CONNECTOR_MOCK_AUTHORISATION_PATH = "/v1/api/charges/authorise";

    WireMockClassRule wireMockClassRule;
    Gson gson = new Gson();
    ObjectMapper objectMapper = new ObjectMapper();

    BaseConnectorMockClient(WireMockClassRule wireMockClassRule) {
        this.wireMockClassRule = wireMockClassRule;
    }

    ImmutableMap<String, String> validGetLink(String href, String rel) {
        return ImmutableMap.of(
                "href", href,
                "rel", rel,
                "method", GET);
    }

    ImmutableMap<String, Object> validPostLink(String href, String rel, String type, Map<String, String> params) {
        return ImmutableMap.of(
                "href", href,
                "rel", rel,
                "type", type,
                "params", params,
                "method", POST);
    }

    String chargeLocation(String accountId, String chargeId) {
        return format(CONNECTOR_MOCK_CHARGE_PATH, accountId, chargeId);
    }
    
    abstract String nextUrlPost();

    String nextUrl(String tokenId) {
        return nextUrlPost() + tokenId;
    }

    void whenGetCharge(String gatewayAccountId, String chargeId, ResponseDefinitionBuilder response) {
        wireMockClassRule.stubFor(get(urlPathEqualTo(format(CONNECTOR_MOCK_CHARGE_PATH, gatewayAccountId, chargeId)))
                .willReturn(response));
    }

    String createChargePayload(CreateChargeRequestParams params) {
        JsonStringBuilder payload = new JsonStringBuilder()
                .add("amount", params.getAmount())
                .add("reference", params.getReference())
                .add("description", params.getDescription())
                .add("return_url", params.getReturnUrl());

        if (!params.getMetadata().isEmpty())
            payload.add("metadata", params.getMetadata());

        if (params.getEmail() != null) {
            payload.add("email", params.getEmail());
        }

        if (params.getCardholderName().isPresent()) {
            payload.addToNestedMap("cardholder_name", params.getCardholderName().get(), "prefilled_cardholder_details");
        }

        if (params.getAddressLine1().isPresent()) {
            payload.addToNestedMap("line1", params.getAddressLine1().get(), "prefilled_cardholder_details", "billing_address");
        }

        if (params.getAddressLine2().isPresent()) {
            payload.addToNestedMap("line2", params.getAddressLine2().get(), "prefilled_cardholder_details", "billing_address");
        }

        if (params.getAddressPostcode().isPresent()) {
            payload.addToNestedMap("postcode", params.getAddressPostcode().get(), "prefilled_cardholder_details", "billing_address");
        }

        if (params.getAddressCity().isPresent()) {
            payload.addToNestedMap("city", params.getAddressCity().get(), "prefilled_cardholder_details", "billing_address");
        }

        if (params.getAddressCountry().isPresent()) {
            payload.addToNestedMap("country", params.getAddressCountry().get(), "prefilled_cardholder_details", "billing_address");
        }

        params.getSetUpAgreement().ifPresent(setUpAgreement -> {
            payload.add("agreement_id", setUpAgreement);
            payload.add("save_payment_instrument_to_agreement", true);
        });

        params.getAgreementId().ifPresent(agreementId -> {
            payload.add("agreement_id", agreementId);
        });

        params.getAuthorisationMode().ifPresent(authorisationMode -> {
            payload.add("authorisation_mode", authorisationMode);
        });

        payload.add("source", params.getSource().orElse(CARD_API));

        return payload.build();
    }

    String createAgreementPayload(CreateAgreementRequestParams params) {
      return new JsonStringBuilder().add("reference", params.getReference()).build();
    }
    
    public void verifyCreateChargeConnectorRequest(String gatewayAccountId, CreateChargeRequestParams createChargeRequestParams) {
        verifyCreateChargeConnectorRequest(gatewayAccountId, createChargePayload(createChargeRequestParams));
    }

    public void verifyCreateChargeConnectorRequest(String gatewayAccountId, String payload) {
        wireMockClassRule.getAllServeEvents();
        wireMockClassRule.verify(1,
                postRequestedFor(urlEqualTo(format(CONNECTOR_MOCK_CHARGES_PATH, gatewayAccountId)))
                        .withRequestBody(equalToJson(payload, true, true)));
    }

    public void verifyCreateChargeConnectorRequestWithHeader(String gatewayAccountId, String payload, StringValuePattern idempotencyKey) {
        wireMockClassRule.getAllServeEvents();
        wireMockClassRule.verify(1,
                postRequestedFor(urlEqualTo(format(CONNECTOR_MOCK_CHARGES_PATH, gatewayAccountId)))
                        .withRequestBody(equalToJson(payload, true, true))
                        .withHeader("Idempotency-Key", idempotencyKey));
    }
    
    public void verifyCreateAgreementConnectorRequest(String gatewayAccountId, CreateAgreementRequestParams createChargeRequestParams) {
        verifyCreateAgreementConnectorRequest(gatewayAccountId, createAgreementPayload(createChargeRequestParams));
    }

    public void verifyCreateAgreementConnectorRequest(String gatewayAccountId, String payload) {
        wireMockClassRule.getAllServeEvents();
        wireMockClassRule.verify(1,
                postRequestedFor(urlEqualTo(format(CONNECTOR_MOCK_AGREEMENTS_PATH, gatewayAccountId)))
                        .withRequestBody(equalToJson(payload, true, true)));
    }
}
