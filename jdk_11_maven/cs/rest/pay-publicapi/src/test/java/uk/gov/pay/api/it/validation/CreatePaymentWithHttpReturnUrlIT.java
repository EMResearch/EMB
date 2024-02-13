package uk.gov.pay.api.it.validation;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.it.rule.RedisDockerRule;
import uk.gov.pay.api.utils.ApiKeyGenerator;
import uk.gov.pay.api.utils.JsonStringBuilder;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;

import java.util.Map;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.mocks.CreateChargeRequestParams.CreateChargeRequestParamsBuilder.aCreateChargeRequestParams;
import static uk.gov.service.payments.commons.testing.port.PortFactory.findFreePort;

public class CreatePaymentWithHttpReturnUrlIT {

    private static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");
    private static final String GATEWAY_ACCOUNT_ID = "GATEWAY_ACCOUNT_ID";
    private static final String PAYMENTS_PATH = "/v1/payments/";

    @ClassRule
    public static RedisDockerRule redisDockerRule = new RedisDockerRule();

    private static final int CONNECTOR_PORT = findFreePort();
    private static final int PUBLIC_AUTH_PORT = findFreePort();

    @ClassRule
    public static WireMockClassRule connectorMock = new WireMockClassRule(CONNECTOR_PORT);

    @ClassRule
    public static WireMockClassRule publicAuthMock = new WireMockClassRule(PUBLIC_AUTH_PORT);

    @ClassRule
    public static DropwizardAppRule<PublicApiConfig> app = new DropwizardAppRule<>(
            PublicApi.class,
            resourceFilePath("config/test-config.yaml"),
            config("connectorUrl", "http://localhost:" + CONNECTOR_PORT),
            config("publicAuthUrl", "http://localhost:" + PUBLIC_AUTH_PORT + "/v1/auth"),
            config("redis.endpoint", redisDockerRule.getRedisUrl()),
            config("allowHttpForReturnUrl", "true")
    );

    private final PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private final ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    
    @Before
    public void setup() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
    }
    
    @Test
    public void createSuccessfullyWhenHttpReturnUrl() {
        String payload = new JsonStringBuilder()
                .add("amount", 100)
                .add("reference", "ref")
                .add("description", "desc")
                .add("return_url", "http://somewhere.com")
                .add("metadata", Map.of())
                .build();

        connectorMockClient.respondCreated_whenCreateCharge(GATEWAY_ACCOUNT_ID, aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription("desc")
                .withReference("ref")
                .withReturnUrl("http://somewhere.com")
                .build());

        postPaymentResponse(payload).statusCode(201);
    }
    
    @Test
    public void createSuccessfullyWhenHttpsReturnUrl() {
        String payload = new JsonStringBuilder()
                .add("amount", 100)
                .add("reference", "ref")
                .add("description", "desc")
                .add("return_url", "https://somewhere.com")
                .add("metadata", Map.of())
                .build();

        connectorMockClient.respondCreated_whenCreateCharge(GATEWAY_ACCOUNT_ID, aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription("desc")
                .withReference("ref")
                .withReturnUrl("https://somewhere.com")
                .build());

        postPaymentResponse(payload).statusCode(201);
    }

    private ValidatableResponse postPaymentResponse(String payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(PAYMENTS_PATH)
                .then();
    }
}
