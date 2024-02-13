package uk.gov.pay.api.it.validation;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.pay.api.it.PaymentResourceITestBase;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.mocks.CreateChargeRequestParams.CreateChargeRequestParamsBuilder.aCreateChargeRequestParams;

@RunWith(JUnitParamsRunner.class)
public class PaymentsResourceLanguageValidationIT extends PaymentResourceITestBase {

    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    
    @Before
    public void setUpBearerToken() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    @Parameters({"en", "cy"})
    public void valid(String language) {
        String payload = toJson(
                Map.of("amount", 100,
                        "reference", "Some ref",
                        "description","hi", 
                        "return_url", "https://somewhere.gov.uk/rainbow/1",
                        "email", "dorothy@rainbow.com",
                        "language", language));

        connectorMockClient.respondCreated_whenCreateCharge(GATEWAY_ACCOUNT_ID, aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription("hi")
                .withReference("Some ref")
                .withReturnUrl("https://somewhere.gov.uk/rainbow/1")
                .build());

        postPaymentResponse(payload).statusCode(201);
    }
    
    @Test
    @Parameters({"fr,422", " ,400", ",400"})
    public void invalidLanguage(String language, int statusCode) {
        String payload = toJson(
                Map.of("amount", 100,
                        "reference", "Some ref",
                        "description","hi", 
                        "return_url", "https://somewhere.gov.uk/rainbow/1",
                        "email", "dorothy@rainbow.com",
                        "language", language));

        postPaymentResponse(payload)
                .statusCode(statusCode)
                .contentType(JSON)
                .body("size()", is(3))
                .body("field", is("language"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: language. Must be \"en\" or \"cy\""));
    }

    @Test
    public void createPayment_responseWith400_whenLanguageIsNumeric() {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 9900,\n" +
                "  \"reference\": \"Some reference\",\n" +
                "  \"description\": \"Some description\",\n" +
                "  \"return_url\": \"https://example.com\",\n" +
                "  \"language\": 1337\n" +
                "}";

        postPaymentResponse(payload)
                .statusCode(400)
                .contentType(JSON)
                .body("size()", is(3))
                .body("field", is("language"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: language. Must be \"en\" or \"cy\""));
    }

    @Test
    public void createPayment_responseWith400_whenLanguageIsNull() {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 9900,\n" +
                "  \"reference\": \"Some reference\",\n" +
                "  \"description\": \"Some description\",\n" +
                "  \"return_url\": \"https://example.com\",\n" +
                "  \"language\": null\n" +
                "}";

        postPaymentResponse(payload)
                .statusCode(400)
                .contentType(JSON)
                .body("size()", is(3))
                .body("field", is("language"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: language. Must be \"en\" or \"cy\""));
    }

    @Test
    public void createPayment_responseWith400_whenLanguageHasNotAValidJsonValue() {
        String payload = "{" +
                "  \"amount\" : 9900," +
                "  \"reference\" : \"Some reference\"," +
                "  \"description\" : \"Some description\"," +
                "  \"return_url\" : \"https://example.com\"," +
                "  \"language\" : " +
                "}";

        postPaymentResponse(payload)
                .statusCode(400)
                .contentType(JSON)
                .body("size()", is(2))
                .body("code", is("P0197"))
                .body("description", is("Unable to parse JSON"));
    }

    @Test
    public void createPayment_responseWith400_whenLanguageFieldIsNotExpectedJsonField() {
        // language=JSON
        String payload = "{\n" +
                "  \"amount\": 9900,\n" +
                "  \"language\": {\n" +
                "    \"whatever\": 1\n" +
                "  },\n" +
                "  \"reference\": \"Some reference\",\n" +
                "  \"description\": \"Some description\",\n" +
                "  \"return_url\": \"https://example.com\"\n" +
                "}";

        postPaymentResponse(payload)
                .statusCode(400)
                .contentType(JSON)
                .body("size()", is(3))
                .body("field", is("language"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: language. Must be \"en\" or \"cy\""));
    }
}
