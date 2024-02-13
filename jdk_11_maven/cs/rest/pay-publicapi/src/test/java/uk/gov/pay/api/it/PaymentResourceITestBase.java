package uk.gov.pay.api.it;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.it.rule.RedisDockerRule;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.utils.ApiKeyGenerator;

import java.util.Map;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static uk.gov.pay.api.utils.Urls.paymentLocationFor;
import static uk.gov.service.payments.commons.testing.port.PortFactory.findFreePort;

public abstract class PaymentResourceITestBase {
    //Must use same secret set in test-config.xml's apiKeyHmacSecret
    protected static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");
    protected static final String GATEWAY_ACCOUNT_ID = "GATEWAY_ACCOUNT_ID";
    protected static final String PAYMENTS_PATH = "/v1/payments/";
    protected static final String AGREEMENTS_PATH = "/v1/agreements/";
    protected static final String LEDGER_ONLY_STRATEGY = "ledger-only";
    protected static final String CONNECTOR_ONLY_STRATEGY = "connector-only";

    @ClassRule
    public static RedisDockerRule redisDockerRule = new RedisDockerRule();

    private static final int CONNECTOR_PORT = findFreePort();
    private static final int PUBLIC_AUTH_PORT = findFreePort();
    private static final int LEDGER_PORT = findFreePort();
    private static final Gson GSON = new GsonBuilder().create();

    @ClassRule
    public static WireMockClassRule connectorMock = new WireMockClassRule(CONNECTOR_PORT);

    @ClassRule
    public static WireMockClassRule publicAuthMock = new WireMockClassRule(PUBLIC_AUTH_PORT);

    @ClassRule
    public static WireMockClassRule ledgerMock = new WireMockClassRule(LEDGER_PORT);

    @Rule
    public DropwizardAppRule<PublicApiConfig> app = new DropwizardAppRule<>(
            PublicApi.class,
            resourceFilePath("config/test-config.yaml"),
            config("connectorUrl", "http://localhost:" + CONNECTOR_PORT),
            config("publicAuthUrl", "http://localhost:" + PUBLIC_AUTH_PORT + "/v1/auth"),
            config("ledgerUrl", "http://localhost:" + LEDGER_PORT),
            config("redis.endpoint", redisDockerRule.getRedisUrl())
    );

    PublicApiConfig configuration;

    @Before
    public void setup() {
        configuration = app.getConfiguration();
        connectorMock.resetAll();
        publicAuthMock.resetAll();
        ledgerMock.resetAll();
    }

    String frontendUrlFor(TokenPaymentType paymentType) {
        return "http://frontend_" + paymentType.toString().toLowerCase() + "/charge/";
    }

    String paymentEventsLocationFor(String chargeId) {
        return paymentLocationFor(configuration.getBaseUrl(), chargeId) + "/events";
    }

    String paymentRefundsLocationFor(String chargeId) {
        return paymentLocationFor(configuration.getBaseUrl(), chargeId) + "/refunds";
    }

    String paymentRefundLocationFor(String chargeId, String refundId) {
        return "http://publicapi.url" + PAYMENTS_PATH + chargeId + "/refunds/" + refundId;
    }

    String paymentCancelLocationFor(String chargeId) {
        return paymentLocationFor(configuration.getBaseUrl(), chargeId) + "/cancel";
    }

    protected ValidatableResponse postPaymentResponse(String payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(PAYMENTS_PATH)
                .then();
    }

    protected static String toJson(Map map) {
        return GSON.toJson(map);
    }
}
