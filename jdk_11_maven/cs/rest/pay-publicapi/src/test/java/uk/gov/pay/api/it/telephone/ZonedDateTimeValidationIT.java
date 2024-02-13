package uk.gov.pay.api.it.telephone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.utils.PublicAuthMockClient;

import java.util.Map;

public class ZonedDateTimeValidationIT extends TelephonePaymentResourceITBase {
    
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    
    @Before
    public void setUpBearerTokenAndRequestBody() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);

        requestBody.put("amount", 100);
        requestBody.put("reference", "Some refeerence");
        requestBody.put("description", "Some description");
        requestBody.put("processor_id", "1PROC");
        requestBody.put("provider_id", "1PROV");
        requestBody.put("payment_outcome", Map.of("status", "success"));
        requestBody.put("card_expiry", "01/99");
        requestBody.put("card_type", "visa");
        requestBody.put("last_four_digits", "1234");
        requestBody.put("first_six_digits", "123456");
    }

    @After
    public void tearDown() {
        requestBody.clear();
    }

    @Test
    public void respondWith422_whenDateIsInvalid() {
        requestBody.put("created_date", "invalid");
        postPaymentResponse(toJson(requestBody))
                .statusCode(422);
    }
}
