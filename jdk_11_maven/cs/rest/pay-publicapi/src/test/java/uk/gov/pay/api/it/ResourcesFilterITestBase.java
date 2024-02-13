package uk.gov.pay.api.it;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.it.rule.RedisDockerRule;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.utils.ApiKeyGenerator;
import uk.gov.pay.api.utils.ChargeEventBuilder;
import uk.gov.pay.api.utils.JsonStringBuilder;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.service.payments.commons.validation.DateTimeUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.jetty.http.HttpStatus.TOO_MANY_REQUESTS_429;
import static org.junit.Assert.fail;
import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;
import static uk.gov.service.payments.commons.testing.port.PortFactory.findFreePort;

abstract public class ResourcesFilterITestBase {

    static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf"); //Must use same secret set in test-config.xml's apiKeyHmacSecret
    static final ZonedDateTime TIMESTAMP = DateTimeUtils.toUTCZonedDateTime("2016-01-01T12:00:00Z").get();
    static final String PAYMENTS_PATH = "/v1/payments/";
    static final int AMOUNT = 9999999;
    static final String CHARGE_ID = "ch_ab2341da231434l";
    static final PaymentState CREATED = new PaymentState("created", false, null, null);
    static final String RETURN_URL = "https://somewhere.gov.uk/rainbow/1";
    static final String REFERENCE = "Some reference";
    static final String DESCRIPTION = "Some description";
    static final String CREATED_DATE = ISO_INSTANT_MILLISECOND_PRECISION.format(TIMESTAMP);
    static final List<Map<String, String>> EVENTS = List.of(new ChargeEventBuilder(CREATED, CREATED_DATE).build());
    static final String GATEWAY_ACCOUNT_ID = "GATEWAY_ACCOUNT_ID";
    static final String PAYLOAD = paymentPayload();

    private static final int CONNECTOR_PORT = findFreePort();
    private static final int PUBLIC_AUTH_PORT = findFreePort();
    private static final int LEDGER_PORT = findFreePort();

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @ClassRule
    public static RedisDockerRule redisDockerRule = new RedisDockerRule();

    @ClassRule
    public static WireMockClassRule connectorMock = new WireMockClassRule(CONNECTOR_PORT);

    @ClassRule
    public static WireMockClassRule ledgerMock = new WireMockClassRule(LEDGER_PORT);

    @ClassRule
    public static WireMockClassRule publicAuthMock = new WireMockClassRule(PUBLIC_AUTH_PORT);

    @Rule
    public DropwizardAppRule<PublicApiConfig> app = new DropwizardAppRule<>(
            PublicApi.class,
            resourceFilePath("config/test-config.yaml"),
            config("connectorUrl", "http://localhost:" + CONNECTOR_PORT),
            config("publicAuthUrl", "http://localhost:" + PUBLIC_AUTH_PORT + "/v1/auth"),
            config("redis.endpoint", redisDockerRule.getRedisUrl()),
            config("ledgerUrl", "http://localhost:" + LEDGER_PORT),
            config("rateLimiter.noOfReq", "1"),
            config("rateLimiter.noOfReqForPost", "1")
    );

    private final PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Before
    public void setupApiKey() {
        redisDockerRule.clearCache();
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    List<ValidatableResponse> invokeAll(List<Callable<ValidatableResponse>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks)
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        fail("Test fail with exception calling resource");
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    TypeSafeMatcher<ValidatableResponse> aResponse(final int statusCode) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(ValidatableResponse validatableResponse) {
                return validatableResponse.extract().statusCode() == statusCode;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(" Status code: ")
                        .appendValue(statusCode);
            }
        };
    }

    TypeSafeMatcher<ValidatableResponse> anErrorResponse() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(ValidatableResponse validatableResponse) {
                ExtractableResponse<Response> extract = validatableResponse.extract();
                return extract.statusCode() == TOO_MANY_REQUESTS_429
                        && "P0900".equals(extract.body().<String>path("code"))
                        && "Too many requests".equals(extract.body().<String>path("description"));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(" Status code: ")
                        .appendValue(TOO_MANY_REQUESTS_429)
                        .appendText(", error code: ")
                        .appendValue("P0900")
                        .appendText(", message: ")
                        .appendValue("Too many requests")
                ;
            }
        };
    }

    private static String paymentPayload() {
        return new JsonStringBuilder()
                .add("amount", (long) ResourcesFilterITestBase.AMOUNT)
                .add("reference", ResourcesFilterITestBase.REFERENCE)
                .add("description", ResourcesFilterITestBase.DESCRIPTION)
                .add("return_url", ResourcesFilterITestBase.RETURN_URL)
                .build();
    }

    ValidatableResponse getPaymentResponse(String bearerToken) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .get(PAYMENTS_PATH + CHARGE_ID)
                .then();
    }

    ValidatableResponse getPaymentEventsResponse(String bearerToken) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .get(String.format("/v1/payments/%s/events", CHARGE_ID))
                .then();
    }

    protected ValidatableResponse postPaymentResponse(String bearerToken, String payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .post(PAYMENTS_PATH)
                .then();
    }

    ValidatableResponse searchPayments(String bearerToken, ImmutableMap<String, String> queryParams) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .queryParams(queryParams)
                .get("/v1/payments")
                .then();
    }

    ValidatableResponse postCancelPaymentResponse(String bearerToken) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .post(String.format("/v1/payments/%s/cancel", CHARGE_ID))
                .then();
    }
}
