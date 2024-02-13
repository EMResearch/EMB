package uk.gov.pay.api.utils.mocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import uk.gov.pay.api.model.links.Link;
import uk.gov.pay.api.utils.JsonStringBuilder;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class LedgerMockClient {

    private final WireMockClassRule ledgerMock;
    private final ObjectMapper mapper = new ObjectMapper();

    public LedgerMockClient(WireMockClassRule ledgerMock) {
        this.ledgerMock = ledgerMock;
        mapper.registerModule(new Jdk8Module());
    }

    public void respondOk_whenSearchCharges(String expectedResponse) {
        whenSearchTransactions(aResponse()
                .withStatus(OK_200)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(expectedResponse));
    }

    public void whenSearchTransactions(ResponseDefinitionBuilder response) {
        ledgerMock.stubFor(get(urlPathEqualTo("/v1/transaction"))
                .withHeader(ACCEPT, matching(APPLICATION_JSON)).willReturn(response));
    }

    public void respondTransactionNotFound(String paymentId, String errorMessage) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", List.of(errorMessage));

        ResponseDefinitionBuilder response = aResponse()
                .withStatus(NOT_FOUND_404)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(new GsonBuilder().create().toJson(payload));

        ledgerMock.stubFor(get(urlPathEqualTo(format("/v1/transaction/%s", paymentId)))
                .willReturn(response));
    }

    public void respondWithGetAllRefunds(String transactionId,
                                         RefundTransactionFromLedgerFixture... refunds) {

        JsonStringBuilder jsonStringBuilder = new JsonStringBuilder()
                .add("parent_transaction_id", transactionId)
                .add("transactions", refunds);

        ledgerMock.stubFor(get(urlPathEqualTo(format("/v1/transaction/%s/transaction", transactionId)))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(jsonStringBuilder.build())));
    }

    public void respondWithSearchRefunds(RefundTransactionFromLedgerFixture... refunds) {

        Map<String, Link> links = (ImmutableMap.of("first_page", new Link("http://server:port/first-link?page=1"),
                "prev_page", new Link("http://server:port/prev-link?page=2"),
                "self", new Link("http://server:port/self-link?page=3"),
                "last_page", new Link("http://server:port/last-link?page=5"),
                "next_page", new Link("http://server:port/next-link?page=4")));

        JsonStringBuilder jsonStringBuilder = new JsonStringBuilder()
                .add("total", 1)
                .add("count", 1)
                .add("page", 1)
                .add("results", Arrays.asList(refunds))
                .add("_links", links);

        ledgerMock.stubFor(get(urlPathEqualTo("/v1/transaction"))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(jsonStringBuilder.build())));
    }

    public void respondWithSearchDisputes(DisputeTransactionFromLedgerFixture... disputes) {

        Map<String, Link> links = (ImmutableMap.of(
                "first_page", new Link("http://server:port/first-link?page=1"),
                "self", new Link("http://server:port/self-link?page=1"),
                "last_page", new Link("http://server:port/last-link?page=1")));

        JsonStringBuilder jsonStringBuilder = new JsonStringBuilder()
                .add("total", 1)
                .add("count", 1)
                .add("page", 1)
                .add("results", Arrays.asList(disputes))
                .add("_links", links);

        ledgerMock.stubFor(get(urlPathEqualTo("/v1/transaction"))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(jsonStringBuilder.build())));
    }

    public void respondWithSearchRefundsNotFound() {
        ledgerMock.stubFor(get(urlPathEqualTo("/v1/transaction"))
                .willReturn(aResponse()
                        .withStatus(NOT_FOUND_404)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public void respondWithSearchDisputesNotFound() {
        ledgerMock.stubFor(get(urlPathEqualTo("/v1/transaction"))
                .willReturn(aResponse()
                        .withStatus(NOT_FOUND_404)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public void respondWithTransaction(String transactionId, TransactionFromLedgerFixture transaction) {
        try {
            var body = mapper.writeValueAsString(transaction);
            respondWithTransaction(transactionId, body);
        } catch (JsonProcessingException e) {
        }
    }

    public void respondWithRefund(String refundId, RefundTransactionFromLedgerFixture refund) {
        try {
            var body = mapper.writeValueAsString(refund);
            respondWithTransaction(refundId, body);
        } catch (JsonProcessingException e) {
        }
    }

    private void respondWithTransaction(String transactionId, String body) {
        ledgerMock.stubFor(get(urlPathEqualTo(format("/v1/transaction/%s", transactionId)))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public void respondWithTransactionEvents(String transactionId, TransactionEventFixture... events) {
        JsonStringBuilder jsonStringBuilder = new JsonStringBuilder()
                .add("transaction_id", transactionId)
                .add("events", Arrays.asList(events));
        String path = format("/v1/transaction/%s/event", transactionId);

        String test = jsonStringBuilder.build();
        ledgerMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(test)));
    }

    public void respondRefundWithError(String refundId) {
        respondTransactionError(refundId, "Downstream system error", INTERNAL_SERVER_ERROR_500);
    }

    public void respondRefundNotFound(String refundId) {
        respondTransactionError(refundId, format("Refund with id [%s] not found.", refundId), BAD_REQUEST_400);
    }

    public void respondTransactionWithError(String paymentId, String errorMessage, int status) {
        respondTransactionError(paymentId, errorMessage, status);
    }

    public void respondTransactionEventsWithError(String transactionId, String errorMessage, int status) {
        String path = format("/v1/transaction/%s/event", transactionId);
        respondError(errorMessage, status, path);
    }

    public void respondWithAgreement(String agreementId, AgreementFromLedgerFixture agreementFromLedgerFixture) throws JsonProcessingException {
        respondWithAgreement(agreementId, mapper.writeValueAsString(agreementFromLedgerFixture));
    }

    public void respondWithAgreement(String agreementId, String body) {
        ledgerMock.stubFor(get(urlPathEqualTo(format("/v1/agreement/%s", agreementId)))
                .withHeader("X-Consistent", WireMock.equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public void respondWithSearchAgreements(String gatewayAccountId, String status, AgreementFromLedgerFixture... agreements) throws JsonProcessingException {
        var links = Map.of(
                "first_page", new Link("http://server:port/first-link?page=1"),
                "prev_page", new Link("http://server:port/prev-link?page=2"),
                "self", new Link("http://server:port/self-link?page=3"),
                "last_page", new Link("http://server:port/last-link?page=5"),
                "next_page", new Link("http://server:port/next-link?page=4")
        );

        var responseBody = new HashMap<String, Object>();
        responseBody.put("total", 9);
        responseBody.put("count", 2);
        responseBody.put("page", 3);
        responseBody.put("results", List.copyOf(Arrays.asList(agreements)));
        responseBody.put("_links", links);

        ledgerMock.stubFor(get(urlPathEqualTo("/v1/agreement"))
                .withQueryParam("page", equalTo("3"))
                .withQueryParam("status", equalTo(status))
                .withQueryParam("account_id", equalTo(gatewayAccountId))
                .withQueryParam("exact_reference_match", equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(OK_200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(mapper.writeValueAsString(responseBody))));
    }

    public void respondAgreementNotFound(String agreementId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", List.of("Agreement not found"));

        ResponseDefinitionBuilder response = aResponse()
                .withStatus(NOT_FOUND_404)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(new GsonBuilder().create().toJson(payload));

        ledgerMock.stubFor(get(urlPathEqualTo(format("/v1/agreement/%s", agreementId)))
                .willReturn(response));
    }

    private void respondTransactionError(String transactionId, String message, int status) {
        String path = format("/v1/transaction/%s", transactionId);
        respondError(message, status, path);
    }

    private void respondError(String message, int status, String path) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", List.of(message));
        payload.put("error_identifier", ErrorIdentifier.GENERIC.toString());

        ledgerMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(new GsonBuilder().create().toJson(payload))));
    }
}
