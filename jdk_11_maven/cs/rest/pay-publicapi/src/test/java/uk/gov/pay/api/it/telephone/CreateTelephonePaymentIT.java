package uk.gov.pay.api.it.telephone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.gov.pay.api.model.telephone.PaymentOutcome;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;

import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static io.restassured.http.ContentType.JSON;

public class CreateTelephonePaymentIT extends TelephonePaymentResourceITBase {
    
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private ConnectorMockClient connectorMockClient = new ConnectorMockClient(connectorMock);

    @Before
    public void setUpBearerTokenAndRequestBody() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);

        requestBody.put("amount", 100);
        requestBody.put("reference", "Some reference");
        requestBody.put("description", "Some description");
        requestBody.put("processor_id", "1PROC");
        requestBody.put("provider_id", "1PROV");
        requestBody.put("payment_outcome", Map.of("status", "success"));

        createTelephonePaymentRequest
                .withAmount(100)
                .withReference("Some reference")
                .withDescription("Some description")
                .withProcessorId("1PROC")
                .withProviderId("1PROV")
                .withPaymentOutcome(new PaymentOutcome("success"));
    }

    @After
    public void tearDown() {
        requestBody.clear();
        createTelephonePaymentRequest
                .withAuthCode(null)
                .withCreatedDate(null)
                .withAuthorisedDate(null)
                .withNameOnCard(null)
                .withEmailAddress(null)
                .withTelephoneNumber(null)
                .withCardExpiry(null)
                .withCardType(null)
                .withLastFourDigits(null)
                .withFirstSixDigits(null);
    }

    @Test
    public void createTelephonePaymentWithAllFields() {
        requestBody.put("auth_code", "666");
        requestBody.put("created_date", "2018-02-21T16:04:25Z");
        requestBody.put("authorised_date", "2018-02-21T16:05:33Z");
        requestBody.put("name_on_card", "Jane Doe");
        requestBody.put("email_address", "jane_doe@example.com");
        requestBody.put("telephone_number", "+447700900796");
        requestBody.put("card_expiry", "01/08");
        requestBody.put("card_type", "visa");
        requestBody.put("last_four_digits", "1234");
        requestBody.put("first_six_digits", "123456");

        createTelephonePaymentRequest
                .withAuthCode("666")
                .withCreatedDate("2018-02-21T16:04:25Z")
                .withAuthorisedDate("2018-02-21T16:05:33Z")
                .withNameOnCard("Jane Doe")
                .withEmailAddress("jane_doe@example.com")
                .withTelephoneNumber("+447700900796")
                .withCardExpiry("01/08")
                .withCardType("visa")
                .withLastFourDigits("1234")
                .withFirstSixDigits("123456");
        
        connectorMockClient.respondCreated_whenCreateTelephoneCharge(GATEWAY_ACCOUNT_ID, createTelephonePaymentRequest
                .build());
        
        postPaymentResponse(toJson(requestBody))
                .statusCode(201)
                .contentType(JSON)
                .body("amount", is(100))
                .body("reference", is("Some reference"))
                .body("description", is("Some description"))
                .body("created_date", is("2018-02-21T16:04:25Z"))
                .body("authorised_date", is("2018-02-21T16:05:33Z"))
                .body("processor_id", is("1PROC"))
                .body("provider_id", is("1PROV"))
                .body("auth_code", is("666"))
                .body("payment_outcome.status", is("success"))
                .body("card_type", is("visa"))
                .body("name_on_card", is("Jane Doe"))
                .body("email_address", is("jane_doe@example.com"))
                .body("card_expiry", is("01/08"))
                .body("last_four_digits", is("1234"))
                .body("first_six_digits", is("123456"))
                .body("telephone_number", is("+447700900796"))
                .body("payment_id", is("dummypaymentid123notpersisted"))
                .body("state.status", is("success"))
                .body("state.finished", is(true));
    }

    @Test
    public void createTelephonePaymentWithRequiredFields() {
        connectorMockClient.respondCreated_whenCreateTelephoneCharge(GATEWAY_ACCOUNT_ID, createTelephonePaymentRequest
                .build());
        
        postPaymentResponse(toJson(requestBody))
                .statusCode(201)
                .contentType(JSON)
                .body("amount", is(100))
                .body("reference", is("Some reference"))
                .body("description", is("Some description"))
                .body("processor_id", is("1PROC"))
                .body("payment_outcome.status", is("success"))
                .body("payment_id", is("dummypaymentid123notpersisted"))
                .body("state.status", is("success"))
                .body("state.finished", is(true));
    }

    @Test
    public void returnExistingTelephonePayment() {
        connectorMockClient.respondOk_whenCreateTelephoneCharge(GATEWAY_ACCOUNT_ID, createTelephonePaymentRequest
                .build());

        postPaymentResponse(toJson(requestBody))
                .statusCode(200)
                .contentType(JSON)
                .body("amount", is(100))
                .body("reference", is("Some reference"))
                .body("description", is("Some description"))
                .body("processor_id", is("1PROC"))
                .body("payment_outcome.status", is("success"))
                .body("card_type", is(nullValue()))
                .body("card_expiry", is(nullValue()))
                .body("last_four_digits", is(nullValue()))
                .body("first_six_digits", is(nullValue()))
                .body("payment_id", is("dummypaymentid123notpersisted"))
                .body("state.status", is("success"))
                .body("state.finished", is(true));
    }

    @Test
    public void telephonePaymentNotificationsNotEnabledForAccount_shouldRespondWith403() {
        connectorMockClient.respondTelephoneNotificationsNotEnabled(GATEWAY_ACCOUNT_ID);

        postPaymentResponse(toJson(requestBody))
                .statusCode(403)
                .contentType(JSON)
                .body("code", is("P0930"))
                .body("description", is("Access to this resource is not enabled for this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ ."));
    }
}
