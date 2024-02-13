package uk.gov.pay.api.service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateRefundException;
import uk.gov.pay.api.model.CardPayment;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.model.RefundFromConnector;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.resources.GetOnePaymentStrategy;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.UriBuilder.fromPath;

public class CreateRefundService {

    private static final Logger logger = LoggerFactory.getLogger(CreateRefundService.class);

    private final GetPaymentService getPaymentService;
    private final Client client;
    private final String connectorUrl;
    private final String baseUrl;

    @Inject
    public CreateRefundService(GetPaymentService getPaymentService,
                               Client client,
                               PublicApiConfig configuration) {
        this.getPaymentService = getPaymentService;
        this.client = client;
        this.baseUrl = configuration.getBaseUrl();
        this.connectorUrl = configuration.getConnectorUrl();
    }

    public RefundResponse createRefund(Account account, String paymentId, CreatePaymentRefundRequest createPaymentRefundRequest) {
        var strategy = new GetOnePaymentStrategy("", account, paymentId, getPaymentService);

        Integer refundAmountAvailable = createPaymentRefundRequest.getRefundAmountAvailable()
                .orElseGet(() -> getRefundAmountAvailableFromPayment(strategy));

        ImmutableMap<String, Object> payloadMap = ImmutableMap.of(
                "amount", createPaymentRefundRequest.getAmount(),
                "refund_amount_available", refundAmountAvailable);
        String connectorPayload = new GsonBuilder().create().toJson(payloadMap);

        Response connectorResponse = client
                .target(getConnectorUrl(format("/v1/api/accounts/%s/charges/%s/refunds", account.getAccountId(), paymentId)))
                .request()
                .post(json(connectorPayload));

        if (connectorResponse.getStatus() != ACCEPTED.getStatusCode()) {
            throw new CreateRefundException(connectorResponse);
        }

        RefundFromConnector refundFromConnector = connectorResponse.readEntity(RefundFromConnector.class);
        logger.debug("created refund returned - [ {} ]", refundFromConnector);
        return RefundResponse.valueOf(refundFromConnector, paymentId, baseUrl);
    }

    private int getRefundAmountAvailableFromPayment(GetOnePaymentStrategy strategy) {
        return Optional.of((CardPayment) strategy.validateAndExecute())
                .map(p -> p.getRefundSummary()
                        .map(RefundSummary::getAmountAvailable)
                        .orElse(0L))
                .map(Long::intValue)
                .orElse(0);
    }

    private String getConnectorUrl(String urlPath) {
        return fromPath(connectorUrl).path(urlPath).toString();
    }
}
