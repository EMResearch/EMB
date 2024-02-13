package uk.gov.pay.api.it;

import au.com.dius.pact.consumer.PactVerification;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.utils.ApiKeyGenerator;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.WiremockStubbing.stubPublicAuthV1ApiAuth;
import static uk.gov.service.payments.commons.testing.port.PortFactory.findFreePort;

public class PaymentsResourceCancelPactConsumerIT {

    private static final String API_KEY = ApiKeyGenerator.apiKeyValueOf("TEST_BEARER_TOKEN", "qwer9yuhgf");

    private final int publicAuthPort = findFreePort();
    
    @Rule
    public WireMockRule publicAuth = new WireMockRule(publicAuthPort);

    @Rule
    public PactProviderRule connector = new PactProviderRule("connector", this);

    @Rule
    public DropwizardAppRule<PublicApiConfig> app = new DropwizardAppRule<>(
            PublicApi.class,
            resourceFilePath("config/test-config.yaml"),
            config("connectorUrl", "http://localhost:" + connector.getConfig().getPort()),
            config("publicAuthUrl", "http://localhost:" + publicAuthPort + "/v1/api/auth"));

    @Before
    public void setup() throws Exception {
        stubPublicAuthV1ApiAuth(publicAuth, new Account("123456", CARD, "a-token-link"), API_KEY);
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-cancel-already-canceled-payment"})
    public void cancelAPaymentThatIsAlreadyCanceled() {
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + API_KEY)
                .post(String.format("/v1/payments/%s/cancel", "charge8133029783750964630"))
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("code", is("P0501"))
                .body("description", is("Cancellation of payment failed"));
    }
}
