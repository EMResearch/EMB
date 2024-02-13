package uk.gov.pay.api.it.validation;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.pay.api.it.PaymentResourceITestBase;
import uk.gov.pay.api.utils.JsonStringBuilder;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.mocks.CreateChargeRequestParams.CreateChargeRequestParamsBuilder.aCreateChargeRequestParams;

@RunWith(JUnitParamsRunner.class)
public class CreatePaymentWithPrefilledCardholderDetailsValidationIT extends PaymentResourceITestBase {

    private final PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private final ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);
    private JsonStringBuilder payload;

    @Before
    public void setUpBearerToken() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
        payload = new JsonStringBuilder()
                .add("amount", 100)
                .add("reference", "Ref")
                .add("description", "hi")
                .add("return_url", "https://somewhere.gov.uk/rainbow/1");
    }

    @Test
    public void shouldFailOnInvalidCardHolderName() {
        payload.add("prefilled_cardholder_details", Map.of("cardholder_name", randomAlphanumeric(256)));
        postPaymentResponse(payload.build())
                .statusCode(422)
                .contentType(JSON)
                .body("size()", is(3))
                .body("field", is("cardholder_name"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: cardholder_name. Must be less than or equal to 255 characters length"));
    }

    @Test
    @Parameters({
            "line1, Must be less than or equal to 255 characters length", 
            "line2, Must be less than or equal to 255 characters length",
            "city, Must be less than or equal to 255 characters length",
            "postcode, Must be less than or equal to 25 characters length"
    })
    public void shouldFailOnInvalidAddress(String addressField, String message) {
        payload.add("prefilled_cardholder_details", Map.of("billing_address", 
                Map.of(addressField, randomAlphanumeric(256))));
        postPaymentResponse(payload.build())
                .statusCode(422)
                .contentType(JSON)
                .body("size()", is(3))
                .body("field", is(addressField))
                .body("code", is("P0102"))
                .body("description", is(format("Invalid attribute value: %s. %s", addressField, message)));
    }

    @Test
    public void shouldCreateSuccessfullyWithEmptyCountry() {
        payload.add("prefilled_cardholder_details", Map.of("billing_address",
                Map.of("country", "")));

        connectorMockClient.respondCreated_whenCreateCharge(GATEWAY_ACCOUNT_ID, aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription("hi")
                .withReference("Ref")
                .withReturnUrl("https://somewhere.gov.uk/rainbow/1")
                .build());

        postPaymentResponse(payload.build()).statusCode(201);
    }
}
