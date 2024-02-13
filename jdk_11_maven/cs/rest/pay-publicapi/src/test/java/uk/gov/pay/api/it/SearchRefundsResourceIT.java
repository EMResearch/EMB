package uk.gov.pay.api.it;


import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.model.ledger.TransactionState;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;
import uk.gov.pay.api.utils.mocks.RefundTransactionFromLedgerFixture;
import uk.gov.service.payments.commons.validation.DateTimeUtils;

import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.Urls.paymentLocationFor;
import static uk.gov.pay.api.utils.mocks.RefundTransactionFromLedgerFixture.RefundTransactionFromLedgerBuilder.aRefundTransactionFromLedgerFixture;
import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class SearchRefundsResourceIT extends PaymentResourceITestBase {

    private static final String CHARGE_ID = "ch_ab2341da231434l";
    private static final ZonedDateTime TIMESTAMP = DateTimeUtils.toUTCZonedDateTime("2016-01-01T12:00:00Z").get();
    private static final String CREATED_DATE = ISO_INSTANT_MILLISECOND_PRECISION.format(TIMESTAMP);

    private LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Before
    public void setUp() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void searchRefunds_shouldReturnValidResponse() {
        RefundTransactionFromLedgerFixture refund1 = aRefundTransactionFromLedgerFixture()
                .withAmount(100L)
                .withCreatedDate(CREATED_DATE)
                .withTransactionId("100")
                .withParentTransactionId(CHARGE_ID)
                .withState(new TransactionState("available", false))
                .build();

        RefundTransactionFromLedgerFixture refund2 = aRefundTransactionFromLedgerFixture()
                .withAmount(300L)
                .withCreatedDate(CREATED_DATE)
                .withTransactionId("300")
                .withSettlementSummary("2020-10-06")
                .withParentTransactionId(CHARGE_ID)
                .withState(new TransactionState("pending", false))
                .build();

        ledgerMockClient.respondWithSearchRefunds(refund1, refund2);

        getRefundsSearchResponse()
                .statusCode(200)
                .contentType(JSON)
                .body("total", is(1))
                .body("count", is(1))
                .body("page", is(1))
                .body("results[0].refund_id", is("100"))
                .body("results[0].created_date", is("2016-01-01T12:00:00.000Z"))
                .body("results[0].payment_id", is("ch_ab2341da231434l"))
                .body("results[0].amount", is(100))
                .body("results[0].status", is("available"))
                .body("results[0]._links.self.href", is(paymentRefundLocationFor(CHARGE_ID, "100")))
                .body("results[0]._links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)))
                .body("results[1].refund_id", is("300"))
                .body("results[1].created_date", is("2016-01-01T12:00:00.000Z"))
                .body("results[1].payment_id", is("ch_ab2341da231434l"))
                .body("results[1].amount", is(300))
                .body("results[1].status", is("pending"))
                .body("results[1].settlement_summary.settled_date", is("2020-10-06"))
                .body("results[1]._links.self.href", is(paymentRefundLocationFor(CHARGE_ID, "300")))
                .body("results[1]._links.payment.href", is(paymentLocationFor(configuration.getBaseUrl(), CHARGE_ID)))
                .body("_links.first_page.href", is("http://publicapi.url/v1/refunds?page=1"))
                .body("_links.prev_page.href", is("http://publicapi.url/v1/refunds?page=2"))
                .body("_links.self.href", is("http://publicapi.url/v1/refunds?page=3"))
                .body("_links.next_page.href", is("http://publicapi.url/v1/refunds?page=4"))
                .body("_links.last_page.href", is("http://publicapi.url/v1/refunds?page=5"));
    }

    @Test
    public void searchRefunds_errorIfLedgerRespondsWith404() {
        ledgerMockClient.respondWithSearchRefundsNotFound();
        
        getRefundsSearchResponse()
                .statusCode(404)
                .contentType(JSON)
                .body("code", is ("P1100"))
                .body("description", is("Page not found"));
    }

    private ValidatableResponse getRefundsSearchResponse() {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .get("/v1/refunds")
                .then();
    }
}
