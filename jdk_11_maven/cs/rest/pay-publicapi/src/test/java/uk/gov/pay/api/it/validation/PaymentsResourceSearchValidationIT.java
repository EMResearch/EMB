package uk.gov.pay.api.it.validation;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonassert.JsonAssert;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.it.PaymentResourceITestBase;
import uk.gov.pay.api.utils.PublicAuthMockClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class PaymentsResourceSearchValidationIT extends PaymentResourceITestBase {

    private static final String VALID_REFERENCE = "test_reference";
    private static final String VALID_LAST_DIGITS_CARD_NUMBER = "4242";
    private static final String VALID_FIRST_DIGITS_CARD_NUMBER = "123456";
    private static final String VALID_STATE = "success";
    private static final String VALID_EMAIL = "alice.111@mail.fake";
    private static final String VALID_FROM_DATE = "2016-01-28T00:00:00Z";
    private static final String VALID_TO_DATE = "2016-01-28T12:00:00Z";
    private static final String INVALID_EMAIL = RandomStringUtils.randomAlphanumeric(254) + "@mail.fake";

    private static final String SEARCH_PATH = "/v1/payments";

    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Before
    public void mapBearerTokenToAccountId() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void searchPayments_errorWhenToDateIsNotInZoneDateTimeFormat() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.<String, String>builder()
                        .put("reference", VALID_REFERENCE)
                        .put("email", VALID_EMAIL)
                        .put("state", VALID_STATE)
                        .put("first_digits_card_number", VALID_FIRST_DIGITS_CARD_NUMBER)
                        .put("last_digits_card_number", VALID_LAST_DIGITS_CARD_NUMBER)
                        .put("from_date", VALID_FROM_DATE)
                        .put("to_date", "2016-01-01 00:00")
                        .build()
        )
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: to_date. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenFromDateIsNotInZoneDateTimeFormat() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.of(
                        "reference", VALID_REFERENCE,
                        "email", VALID_EMAIL,
                        "state", VALID_STATE,
                        "from_date", "2016-01-01 00:00",
                        "to_date", VALID_TO_DATE))
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: from_date. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenStatusNotMatchingWithExpectedExternalStatuses() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.of(
                        "reference", VALID_REFERENCE,
                        "email", VALID_EMAIL,
                        "state", "invalid state",
                        "from_date", VALID_FROM_DATE,
                        "to_date", VALID_TO_DATE))
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: state. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenReferenceSizeIsLongerThan255() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.of(
                        "reference", RandomStringUtils.randomAlphanumeric(256),
                        "email", VALID_EMAIL,
                        "state", VALID_STATE,
                        "from_date", VALID_FROM_DATE,
                        "to_date", VALID_TO_DATE))
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: reference. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenEmailSizeIsLongerThan254() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.of(
                        "reference", "ref",
                        "email", RandomStringUtils.randomAlphanumeric(254) + "@mail.fake",
                        "state", VALID_STATE,
                        "from_date", VALID_FROM_DATE,
                        "to_date", VALID_TO_DATE))
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: email. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenToDateNotInZoneDateTimeFormat_andInvalidStatus() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.of(
                        "reference", VALID_REFERENCE,
                        "email", VALID_EMAIL,
                        "state", "invalid state",
                        "from_date", VALID_FROM_DATE,
                        "to_date", "2016-01-01 00:00"))
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: state, to_date. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenFromAndToDatesAreNotInZoneDateTimeFormat() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.of(
                        "reference", VALID_REFERENCE,
                        "email", VALID_EMAIL,
                        "state", VALID_STATE,
                        "from_date", "12345",
                        "to_date", "2016-01-01 00:00"))
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: from_date, to_date. See Public API documentation for the correct data formats"));
    }
    
    @Test
    public void searchPayments_errorWhenCardDigitsAreInvalid() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.<String, String>builder()
                        .put("reference", VALID_REFERENCE)
                        .put("email", VALID_EMAIL)
                        .put("state", VALID_STATE)
                        .put("first_digits_card_number", "4a4234")
                        .put("last_digits_card_number", "423")
                        .put("from_date", VALID_FROM_DATE)
                        .put("to_date", VALID_TO_DATE)
                        .build()
        )
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        String json = IOUtils.toString(body, StandardCharsets.UTF_8);

        JsonAssert.with(json)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: first_digits_card_number, last_digits_card_number. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenAllFieldsAreInvalid() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.of(
                        "reference", RandomStringUtils.randomAlphanumeric(256),
                        "email", INVALID_EMAIL,
                        "state", "invalid state",
                        "from_date", "12345",
                        "to_date", "98765"))
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        String json = IOUtils.toString(body, StandardCharsets.UTF_8);

        JsonAssert.with(json)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: state, reference, email, from_date, to_date. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPayments_errorWhenDisplaySizeInvalid() throws Exception {
        InputStream body = searchPayments(API_KEY,
                ImmutableMap.<String, String>builder()
                        .put("reference", VALID_REFERENCE)
                        .put("email", VALID_EMAIL)
                        .put("state", VALID_STATE)
                        .put("from_date", VALID_FROM_DATE)
                        .put("to_date", VALID_TO_DATE)
                        .put("display_size", "501")
                        .build())
                .statusCode(422)
                .contentType(JSON).extract()
                .body().asInputStream();

        JsonAssert.with(body)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.code", is("P0401"))
                .assertThat("$.description", is("Invalid parameters: display_size. See Public API documentation for the correct data formats"));
    }

    private ValidatableResponse searchPayments(String bearerToken, ImmutableMap<String, String> queryParams) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .queryParams(queryParams)
                .get(SEARCH_PATH)
                .then();
    }
}
