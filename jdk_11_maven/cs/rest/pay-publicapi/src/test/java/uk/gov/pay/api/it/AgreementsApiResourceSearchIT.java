package uk.gov.pay.api.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.ConnectorMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.mocks.AgreementFromLedgerFixture.AgreementFromLedgerFixtureBuilder.anAgreementFromLedgerWithPaymentInstrumentFixture;
import static uk.gov.pay.api.utils.mocks.AgreementFromLedgerFixture.AgreementFromLedgerFixtureBuilder.anAgreementFromLedgerWithoutPaymentInstrumentFixture;

public class AgreementsApiResourceSearchIT extends PaymentResourceITestBase {

    private final PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);
    private final LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);

    @Before
    public void setUp() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }


    @Test
    public void searchAgreementsFromLedger() throws JsonProcessingException {
        String agreementId = "an-agreement-id";
        var agreementWithPaymentInstrument = anAgreementFromLedgerWithPaymentInstrumentFixture()
                .withExternalId(agreementId)
                .withUserIdentifier("a-valid-user-identifier")
                .build();

        var agreementWithoutPaymentInstrument = anAgreementFromLedgerWithoutPaymentInstrumentFixture()
                .withExternalId("another-agreement-id")
                .withServiceId("service-2")
                .withReference("ref-2")
                .withDescription("Description 2")
                .withStatus("CREATED")
                .withCreatedDate("2022-07-27T12:30:00Z")
                .build();

        ledgerMockClient.respondWithSearchAgreements(GATEWAY_ACCOUNT_ID, "created", agreementWithPaymentInstrument, agreementWithoutPaymentInstrument);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .basePath(AGREEMENTS_PATH)
                .queryParam("status", "created")
                .queryParam("page", "3")
                .get()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("total", is(9))
                .body("count", is(2))
                .body("page", is(3))
                .body("_links.self.href", containsString("/v1/agreements?page=3"))
                .body("_links.first_page.href", containsString("/v1/agreements?page=1"))
                .body("_links.last_page.href", containsString("/v1/agreements?page=5"))
                .body("_links.prev_page.href", containsString("/v1/agreements?page=2"))
                .body("_links.next_page.href", containsString("/v1/agreements?page=4"))
                .body("results.size()", is(2))
                .body("results[0].agreement_id", is(agreementWithPaymentInstrument.getExternalId()))
                .body("results[0].reference", is(agreementWithPaymentInstrument.getReference()))
                .body("results[0].description", is(agreementWithPaymentInstrument.getDescription()))
                .body("results[0].user_identifier", is(agreementWithPaymentInstrument.getUserIdentifier()))
                .body("results[0].status", is(agreementWithPaymentInstrument.getStatus().toLowerCase()))
                .body("results[0].created_date", is(agreementWithPaymentInstrument.getCreatedDate()))
                .body("results[0].payment_instrument.type", is(agreementWithPaymentInstrument.getPaymentInstrument().getType()))
                .body("results[0].payment_instrument.card_details.card_type",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getCardType()))
                .body("results[0].payment_instrument.card_details.card_brand",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getCardBrand()))
                .body("results[0].payment_instrument.card_details.cardholder_name",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getCardHolderName()))
                .body("results[0].payment_instrument.card_details.billing_address.line1",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getBillingAddress().get().getLine1()))
                .body("results[0].payment_instrument.card_details.billing_address.line2",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getBillingAddress().get().getLine2()))
                .body("results[0].payment_instrument.card_details.billing_address.city",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getBillingAddress().get().getCity()))
                .body("results[0].payment_instrument.card_details.billing_address.postcode",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getBillingAddress().get().getPostcode()))
                .body("results[0].payment_instrument.card_details.billing_address.country",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getBillingAddress().get().getCountry()))
                .body("results[0].payment_instrument.card_details.expiry_date",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getExpiryDate()))
                .body("results[0].payment_instrument.card_details.first_digits_card_number",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getFirstDigitsCardNumber()))
                .body("results[0].payment_instrument.card_details.last_digits_card_number",
                        is(agreementWithPaymentInstrument.getPaymentInstrument().getCardDetails().getLastDigitsCardNumber()))
                .body("results[0].payment_instrument.created_date", is(agreementWithPaymentInstrument.getPaymentInstrument().getCreatedDate()))
                .body("results[1].agreement_id", is(agreementWithoutPaymentInstrument.getExternalId()))
                .body("results[1].reference", is(agreementWithoutPaymentInstrument.getReference()))
                .body("results[1].description", is(agreementWithoutPaymentInstrument.getDescription()))
                .body("results[1].status", is(agreementWithoutPaymentInstrument.getStatus().toLowerCase()))
                .body("results[1].created_date", is(agreementWithoutPaymentInstrument.getCreatedDate()))
                .body("results[1]", not(hasKey("payment_instrument")));
    }

    @Test
    public void searchCancelledAgreementsFromLedger() throws JsonProcessingException {
        String agreementId = "an-agreement-id";
       var agreementWithoutPaymentInstrument = anAgreementFromLedgerWithoutPaymentInstrumentFixture()
               .withExternalId("another-agreement-id")
               .withServiceId("service-2")
               .withReference("ref-2")
               .withDescription("Description 2")
               .withStatus("CANCELLED")
               .withCreatedDate("2022-07-27T12:30:00Z")
               .withCancelledDate("2023-06-14T16:52:00Z")
               .build();

        ledgerMockClient.respondWithSearchAgreements(GATEWAY_ACCOUNT_ID, "cancelled", agreementWithoutPaymentInstrument);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .basePath(AGREEMENTS_PATH)
                .queryParam("status", "cancelled")
                .queryParam("page", "3")
                .get()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("total", is(9))
                .body("count", is(2))
                .body("page", is(3))
                .body("_links.self.href", containsString("/v1/agreements?page=3"))
                .body("_links.first_page.href", containsString("/v1/agreements?page=1"))
                .body("_links.last_page.href", containsString("/v1/agreements?page=5"))
                .body("_links.prev_page.href", containsString("/v1/agreements?page=2"))
                .body("_links.next_page.href", containsString("/v1/agreements?page=4"))
                .body("results.size()", is(1))
                .body("results[0].agreement_id", is(agreementWithoutPaymentInstrument.getExternalId()))
                .body("results[0].reference", is(agreementWithoutPaymentInstrument.getReference()))
                .body("results[0].description", is(agreementWithoutPaymentInstrument.getDescription()))
                .body("results[0].status", is(agreementWithoutPaymentInstrument.getStatus().toLowerCase()))
                .body("results[0].created_date", is(agreementWithoutPaymentInstrument.getCreatedDate()))
                .body("results[0]", not(hasKey("payment_instrument")));
    }

    @Test
    public void searchAgreementsReturnsErrorWhenValidationError() {
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .basePath(AGREEMENTS_PATH)
                .queryParam("status", "ethereal")
                .get()
                .then()
                .statusCode(422)
                .contentType(ContentType.JSON)
                .body("code", is("P2401"))
                .body("description", is("Invalid parameters: status. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchPaymentsAgreements_errorIfLedgerRespondsWith404() {
        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .basePath(AGREEMENTS_PATH)
                .get()
                .then()
                .statusCode(404)
                .body("code", is("P2402"))
                .body("description", is("Page not found"));
    }
}
