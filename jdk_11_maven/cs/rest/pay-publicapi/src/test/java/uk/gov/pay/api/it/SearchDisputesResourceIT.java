package uk.gov.pay.api.it;

import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.api.model.ledger.DisputeSettlementSummary;
import uk.gov.pay.api.model.ledger.TransactionState;
import uk.gov.pay.api.utils.PublicAuthMockClient;
import uk.gov.pay.api.utils.mocks.LedgerMockClient;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.api.utils.mocks.DisputeTransactionFromLedgerFixture.DisputeTransactionFromLedgerBuilder.aDisputeTransactionFromLedgerFixture;

public class SearchDisputesResourceIT extends PaymentResourceITestBase {

    private static final String SEARCH_PATH = "/v1/disputes";
    private LedgerMockClient ledgerMockClient = new LedgerMockClient(ledgerMock);
    private PublicAuthMockClient publicAuthMockClient = new PublicAuthMockClient(publicAuthMock);

    @Before
    public void setUp() {
        publicAuthMockClient.mapBearerTokenToAccountId(API_KEY, GATEWAY_ACCOUNT_ID);
    }

    @Test
    public void searchDisputes_shouldReturnValidResponse() {
        var dispute = aDisputeTransactionFromLedgerFixture()
                .withTransactionId("a-transaction-id")
                .withAmount(1000L)
                .withCreatedDate("2022-05-20T19:05:00.000Z")
                .withEvidenceDueDate("2022-05-27T19:05:00.000Z")
                .withFee(1500L)
                .withNetAmount(-2500L)
                .withParentTransactionId("a-parent-transaction-id")
                .withReason("fraudulent")
                .withSettlementSummary(new DisputeSettlementSummary("2022-05-27"))
                .withState(new TransactionState("lost", true))
                .build();

        ledgerMockClient.respondWithSearchDisputes(dispute);

        searchDisputes(Map.of())
                .statusCode(200)
                .contentType(JSON)
                .body("page", is(1))
                .body("total", is(1))
                .body("count", is(1))
                .body("links.self.href", containsString("/v1/disputes"))
                .body("links.first_page.href", containsString("/v1/disputes"))
                .body("links.last_page.href", containsString("/v1/disputes"))

                .body("results[0].amount", is(1000))
                .body("results[0].fee", is(1500))
                .body("results[0].reason", is("fraudulent"))
                .body("results[0].status", is("lost"))
                .body("results[0].created_date", is("2022-05-20T19:05:00.000Z"))
                .body("results[0].dispute_id", is("a-transaction-id"))
                .body("results[0].evidence_due_date", is("2022-05-27T19:05:00.000Z"))
                .body("results[0].net_amount", is(-2500))
                .body("results[0].payment_id", is("a-parent-transaction-id"))
                .body("results[0].settlement_summary.settled_date", is("2022-05-27"))
                .body("results[0]._links.payment.href", containsString("/v1/payments/a-parent-transaction-id"))
                .body("results[0]._links.payment.method", is("GET"));
    }

    @Test
    public void shouldError_whenInvalidSearchParams() {
        Map<String, String> queryParams = Map.of(
                "from_date", "27th of July, 2022, 11:23",
                "to_date", "27th of July, 2022, 13:05",
                "from_settled_date", "3rd of July",
                "to_settled_date", "30th of July",
                "status", "disputed",
                "page", "second",
                "display_size", "short"
        );

        searchDisputes(queryParams)
                .statusCode(422)
                .contentType(JSON)
                .body("code", is("P0401"))
                .body("description", is("Invalid parameters: from_date, to_date, from_settled_date, to_settled_date, page, display_size, state. See Public API documentation for the correct data formats"));
    }

    @Test
    public void searchDisputesShouldReturnPageNotFound_whenLedgerRespondsWith404() {
        ledgerMockClient.respondWithSearchDisputesNotFound();

        searchDisputes(Map.of(
                "status", "lost",
                "page", "2"))
                .statusCode(404)
                .contentType(JSON)
                .body("code", is ("P0402"))
                .body("description", is("Page not found"));
    }

    @Test
    public void searchDisputes_shouldReturns401WhenUnauthorised() {
        publicAuthMockClient.respondUnauthorised();

        searchDisputes(Map.of())
                .statusCode(401);
    }

    private ValidatableResponse searchDisputes(Map<String, String> queryParams) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .header(AUTHORIZATION, "Bearer " + PaymentResourceITestBase.API_KEY)
                .queryParams(queryParams)
                .get(SEARCH_PATH)
                .then();
    }
}
