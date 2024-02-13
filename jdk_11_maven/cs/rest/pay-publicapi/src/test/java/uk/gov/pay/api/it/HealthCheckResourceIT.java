package uk.gov.pay.api.it;

import io.restassured.RestAssured;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static uk.gov.pay.api.resources.HealthCheckResource.HEALTHCHECK;

public class HealthCheckResourceIT extends PaymentResourceITestBase {

    @Test
    public void getAccountShouldReturn404IfAccountIdIsUnknown() {
        RestAssured.given().port(app.getLocalPort())
                .get(HEALTHCHECK)
                .then()
                .statusCode(200)
                .body("ping.healthy", is(true))
                .body("deadlocks.healthy", is(true));
    }
}
