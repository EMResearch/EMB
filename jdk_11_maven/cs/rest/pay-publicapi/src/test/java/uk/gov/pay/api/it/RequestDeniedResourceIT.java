package uk.gov.pay.api.it;

import com.jayway.jsonassert.JsonAssert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class RequestDeniedResourceIT extends PaymentResourceITestBase {

    @Test
    public void requestDeniedPost() throws IOException {

        InputStream body = given().port(app.getLocalPort())
                .header("x-naxsi_sig", "rules violated")
                .post("request-denied")
                .then()
                .statusCode(400)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0920"))
                .assertThat("$.description", is("Request blocked by security rules. Please consult API documentation for more information."));

    }

    @Test
    public void requestDeniedGet() throws IOException {

        InputStream body = given().port(app.getLocalPort())
                .header("x-naxsi_sig", "rules violated")
                .get("request-denied")
                .then()
                .statusCode(400)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0920"))
                .assertThat("$.description", is("Request blocked by security rules. Please consult API documentation for more information."));

    }

    @Test
    public void requestDeniedPut() throws IOException {

        InputStream body = given().port(app.getLocalPort())
                .header("x-naxsi_sig", "rules violated")
                .put("request-denied")
                .then()
                .statusCode(400)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0920"))
                .assertThat("$.description", is("Request blocked by security rules. Please consult API documentation for more information."));

    }

    @Test
    public void requestDeniedDelete() throws IOException {

        InputStream body = given().port(app.getLocalPort())
                .header("x-naxsi_sig", "rules violated")
                .delete("request-denied")
                .then()
                .statusCode(400)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0920"))
                .assertThat("$.description", is("Request blocked by security rules. Please consult API documentation for more information."));

    }
}
