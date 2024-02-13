package uk.gov.pay.api.it.validation;

import com.fasterxml.jackson.databind.JsonNode;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.pay.api.it.PaymentResourceITestBase;
import uk.gov.pay.api.utils.JsonStringBuilder;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.CreateChargeRequestParams;
import uk.gov.pay.api.utils.mocks.CreateChargeRequestParams.CreateChargeRequestParamsBuilder;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static io.restassured.http.ContentType.JSON;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.it.PaymentsResourceCreateIT.paymentPayload;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;
import static uk.gov.pay.api.utils.mocks.CreateChargeRequestParams.CreateChargeRequestParamsBuilder.aCreateChargeRequestParams;

@RunWith(JUnitParamsRunner.class)
public class PaymentsResourceMetadataValidationFailuresIT extends PaymentResourceITestBase {

    private static CreateChargeRequestParamsBuilder createChargeRequestParamsBuilder = aCreateChargeRequestParams()
                .withAmount(100)
                .withDescription("DESCRIPTION")
                .withReference("REFERENCE")
                .withReturnUrl("https://somewhere.gov.uk/rainbow/1");
    
    private static final  String TOO_LONG_KEY = IntStream.rangeClosed(1, ExternalMetadata.MAX_KEY_LENGTH + 1).mapToObj(i -> "k").collect(joining());
    private static final String TOO_LONG_VALUE = IntStream.rangeClosed(1, ExternalMetadata.MAX_VALUE_LENGTH + 1).mapToObj(i -> "v").collect(joining());


    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    
    @Before
    public void before() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID, CARD);
    }

    @Test
    public void valueIsNotAStringBooleanOrNumber() {
        CreateChargeRequestParams createChargeRequestParams = createChargeRequestParamsBuilder
                .withMetadata(Map.of("foo", List.of("cake", "chocolate"), "bar", Map.of("a", "b")))
                .build();

        assertMetadataValidationError(createChargeRequestParams, 
                "Invalid attribute value: metadata. Values must be of type String, Boolean or Number");
    }

    @Test
    @Parameters({"", " "})
    public void keyIsInvalid(String key) {
        var createChargeRequestParams = createChargeRequestParamsBuilder.withMetadata(Map.of(key, "boo")).build();

        assertMetadataValidationError(createChargeRequestParams, 
                "Invalid attribute value: metadata. Keys must be between " + ExternalMetadata.MIN_KEY_LENGTH + " and "
                        + ExternalMetadata.MAX_KEY_LENGTH + " characters long");
    }

    @Test
    public void keyIsTooLong() {
        var createChargeRequestParams = createChargeRequestParamsBuilder.withMetadata(Map.of(TOO_LONG_KEY, "boo")).build();

        assertMetadataValidationError(createChargeRequestParams,
                "Invalid attribute value: metadata. Keys must be between " + ExternalMetadata.MIN_KEY_LENGTH + " and "
                        + ExternalMetadata.MAX_KEY_LENGTH + " characters long");
    }
    
    @Test
    public void valueIsNull() {
        Map<String, Object> metadata = new HashMap<>() {{ put("key", null); }};
        var createChargeRequestParams = createChargeRequestParamsBuilder.withMetadata(metadata).build();

        assertMetadataValidationError(createChargeRequestParams, "Invalid attribute value: metadata. Must not have null values");
    }

    @Test
    public void valueIsTooLong() {
        var createChargeRequestParams = createChargeRequestParamsBuilder.withMetadata(Map.of("key", TOO_LONG_VALUE)).build();

        assertMetadataValidationError(createChargeRequestParams,
                "Invalid attribute value: metadata. Values must be no greater than " + ExternalMetadata.MAX_VALUE_LENGTH + " characters long");
    }

    @Test
    public void moreThanMaxKeyValuePairs() {
        Map<String, Object> metadata = IntStream.rangeClosed(1, ExternalMetadata.MAX_KEY_VALUE_PAIRS + 1)
                .boxed().collect(toUnmodifiableMap(i -> "key " + i, i -> "value " + i));

        var createChargeRequestParams = createChargeRequestParamsBuilder.withMetadata(metadata).build();

        assertMetadataValidationError(createChargeRequestParams, 
                "Invalid attribute value: metadata. Cannot have more than " + ExternalMetadata.MAX_KEY_VALUE_PAIRS + " key-value pairs");
    }
    
    @Test
    public void metadataIsNotAnObject() {
        var createChargeRequestParams = createChargeRequestParamsBuilder.build();
        
        JsonStringBuilder payload = new JsonStringBuilder()
                .add("amount", createChargeRequestParams.getAmount())
                .add("reference", createChargeRequestParams.getReference())
                .add("description", createChargeRequestParams.getDescription())
                .add("return_url", createChargeRequestParams.getReturnUrl())
                .add("metadata", "something");

        postPaymentResponse(payload.build())
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(JSON)
                .body("field", is("metadata"))
                .body("code", is("P0102"))
                .body("description", is("Invalid attribute value: metadata. Must be an object of JSON key-value pairs"));
    }

    @Test
    public void testMultipleValidationErrors() {
        var metadata = Map.of(
                "key", TOO_LONG_VALUE,
                TOO_LONG_KEY, "fuh",
                "keyForBadValue", List.of("cake", "chocolate")
        );

        var createChargeRequestParams = createChargeRequestParamsBuilder.withMetadata(metadata).build();

        JsonNode jsonBody = postPaymentResponse(paymentPayload(createChargeRequestParams))
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(JSON)
                .body("field", is("metadata"))
                .body("code", is("P0102"))
                .extract().body().as(JsonNode.class);

        var descriptions = asList(jsonBody.get("description").asText()
                .replace("Invalid attribute value: metadata. ", "")
                .split("\\. "));
        assertThat(descriptions, hasItems(
                "Values must be no greater than " + ExternalMetadata.MAX_VALUE_LENGTH + " characters long",
                "Keys must be between " + ExternalMetadata.MIN_KEY_LENGTH + " and " + ExternalMetadata.MAX_KEY_LENGTH + " characters long",
                "Values must be of type String, Boolean or Number"
        ));
    }

    private void assertMetadataValidationError(CreateChargeRequestParams createChargeRequestParams, String message) {
        postPaymentResponse(paymentPayload(createChargeRequestParams))
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(JSON)
                .body("field", is("metadata"))
                .body("code", is("P0102"))
                .body("description", is(message));
    }
}
