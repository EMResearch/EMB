package uk.gov.pay.api.resources;

import com.google.gson.GsonBuilder;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;
import uk.gov.pay.api.it.PaymentResourceITestBase;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.core.Is.is;

public class AuthorisationResourceValidationIT extends PaymentResourceITestBase {

    private static final String AUTH_PATH = "/v1/auth";

    @Test
    public void authorisation_responseWith422_whenOneTimeTokenFieldIsNumeric() {
        String payload = toJson(
                Map.of("one_time_token", 1234567890,
                        "card_number", "1234567890123456",
                        "cvc", "123",
                        "expiry_date", "09/27",
                        "cardholder_name", "Joe Boggs"));

        postAuthRequest(payload)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0102"))
                .body("field", is("one_time_token"))
                .body("description", is("Invalid attribute value: one_time_token. Must be of type String"));
    }

    @Test
    public void authorisation_responseWith422_whenOneTimeTokenFieldIsMissing() {
        String payload = toJson(
                Map.of("card_number", "1234567890123456",
                        "cvc", "123",
                        "expiry_date", "09/27",
                        "cardholder_name", "Joe Boggs"));

        postAuthRequest(payload)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0101"))
                .body("field", is("one_time_token"))
                .body("description", is("Missing mandatory attribute: one_time_token"));
    }

    @Test
    public void authorisation_responseWith422_whenCardNumberFieldHasNullValue() {
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("one_time_token", "1234567890");
        payloadMap.put("card_number", "1234567890123456");
        payloadMap.put("cvc", null);
        payloadMap.put("expiry_date", "09/27");
        payloadMap.put("cardholder_name", "Joe Boggs");

        String payload = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(payloadMap);


        postAuthRequest(payload)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0101"))
                .body("field", is("cvc"))
                .body("description", is("Missing mandatory attribute: cvc"));
    }

    @Test
    public void authorisation_responseWith422_whenCardholderNameIsEmpty() {
        String payload = toJson(
                Map.of("one_time_token", "1234567890",
                        "card_number", "1234567890123456",
                        "cvc", "123",
                        "expiry_date", "09/27",
                        "cardholder_name", ""));

        postAuthRequest(payload)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0101"))
                .body("field", is("cardholder_name"))
                .body("description", is("Missing mandatory attribute: cardholder_name"));
    }

    @Test
    public void authorisation_responseWith422_whenCvcIsEmpty() {
        String payload = toJson(
                Map.of("one_time_token", "1234567890",
                        "card_number", "1234567890123456",
                        "cvc", "",
                        "expiry_date", "09/27",
                        "cardholder_name", "Joe Boggs"));

        postAuthRequest(payload)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0102"))
                .body("field", is("cvc"))
                .body("description", is("Invalid attribute value: cvc. Must be between 3 and 4 characters long"));
    }

    @Test
    public void authorisation_responseWith422_whenCardholderNameIsTooLong() {
        String payload = toJson(
                Map.of("one_time_token", "1234567890",
                        "card_number", "1234567890123456",
                        "cvc", "123",
                        "expiry_date", "09/27",
                        "cardholder_name", "Joe Boggs".repeat(29)));

        postAuthRequest(payload)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0102"))
                .body("field", is("cardholder_name"))
                .body("description", is("Invalid attribute value: cardholder_name. Must be less than or equal to 255 characters long"));
    }

    @Test
    public void authorisation_responseWith422_whenExpiryDateIsTooLong() {
        String payload = toJson(
                Map.of("one_time_token", "1234567890",
                        "card_number", "1234567890123456",
                        "cvc", "123",
                        "expiry_date", "Sep 28",
                        "cardholder_name", "Joe Boggs"));

        postAuthRequest(payload)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0102"))
                .body("field", is("expiry_date"))
                .body("description", is("Invalid attribute value: expiry_date. Must be a valid date with the format MM/YY"));
    }

    protected ValidatableResponse postAuthRequest(String payload) {
        return given().port(app.getLocalPort())
                .body(payload)
                .accept(JSON)
                .contentType(JSON)
                .post(AUTH_PATH)
                .then();
    }
}
