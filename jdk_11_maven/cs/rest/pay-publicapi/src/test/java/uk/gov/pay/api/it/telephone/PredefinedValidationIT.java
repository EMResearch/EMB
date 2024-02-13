package uk.gov.pay.api.it.telephone;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.utils.PublicAuthMockClient;

import java.util.Map;

public class PredefinedValidationIT extends TelephonePaymentResourceITBase {

    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Before
    public void setUpBearerTokenAndRequestBody() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);

        requestBody.put("amount", 100);
        requestBody.put("reference", "Some reference");
        requestBody.put("description", "Some description");
        requestBody.put("processor_id", "1PROC");
        requestBody.put("provider_id", "1PROV");
        requestBody.put("payment_outcome", Map.of("status", "success"));
        requestBody.put("card_type", "visa");
        requestBody.put("card_expiry", "01/08");
        requestBody.put("last_four_digits", "1234");
        requestBody.put("first_six_digits", "123456");
    }

    @After
    public void tearDown() {
        requestBody.clear();
    }

    @Test
    public void respondWith422_whenReferenceLengthIsGreaterThanMaxValue() {
        requestBody.replace("reference", StringUtils.repeat("*", 256));
        postPaymentResponse(toJson(requestBody))
                .statusCode(422);
    }

    @Test
    public void respondWith422_whenDescriptionLengthIsGreaterThanMaxValue() {
        requestBody.replace("description", StringUtils.repeat("*", 256));
        postPaymentResponse(toJson(requestBody))
                .statusCode(422);
    }

    @Test
    public void respondWith422_whenProcessorIdIsMissing() {
        requestBody.remove("processor_id");
        postPaymentResponse(toJson(requestBody))
                .statusCode(422);
    }

    @Test
    public void respondWith422_whenProcessorIdIsNull() {
        requestBody.replace("processor_id", null);
        postPaymentResponse(toJson(requestBody))
                .statusCode(422);
    }

    @Test
    public void respondWith422_whenProviderIdIsMissing() {
        requestBody.remove("provider_id");
        postPaymentResponse(toJson(requestBody))
                .statusCode(422);
    }

    @Test
    public void respondWith422_whenProviderIdIsNull() {
        requestBody.replace("provider_id", null);
        postPaymentResponse(toJson(requestBody))
                .statusCode(422);
    }
}
