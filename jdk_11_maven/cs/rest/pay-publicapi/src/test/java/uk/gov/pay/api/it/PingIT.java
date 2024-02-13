package uk.gov.pay.api.it;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.api.app.PublicApi;
import uk.gov.pay.api.app.config.PublicApiConfig;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class PingIT {

    @Rule
    public DropwizardAppRule<PublicApiConfig> app = new DropwizardAppRule<>(PublicApi.class, resourceFilePath("config/test-config.yaml"));

    @Test
    public void testPing() {
        given().port(app.getAdminPort())
                .get("/healthcheck")
                .then()
                .body("ping.healthy", is(true));
    }
}
