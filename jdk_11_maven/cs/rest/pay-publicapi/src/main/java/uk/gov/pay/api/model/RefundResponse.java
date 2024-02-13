package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.RefundLinksForSearch;
import uk.gov.pay.api.model.ledger.RefundTransactionFromLedger;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "Refund")
public class RefundResponse {

    @Schema(example = "act4c33g40j3edfmi8jknab84x", 
            description = "The unique ID GOV.UK Pay automatically associated with this refund when you created it.",
            accessMode = READ_ONLY)
    private String refundId;
    @Schema(example = "2017-01-10T16:52:07.855Z", 
            description = "The date and time you created this refund. " + 
                    "This value uses Coordinated Universal Time (UTC) and ISO 8601 format - `YYYY-MM-DDThh:mm:ss.SSSZ`", 
            accessMode = READ_ONLY)
    private String createdDate;
    @Schema(example = "120", description = "The amount refunded to the user in pence.", accessMode = READ_ONLY)
    private Long amount;
    @JsonProperty("_links")
    private RefundLinksForSearch links;
    @Schema(example = "success", 
            description = "The [status of the refund](https://docs.payments.service.gov.uk/refunding_payments/#checking-the-status-of-a-refund-status).",
            allowableValues = {"submitted", "success", "error"}, accessMode = READ_ONLY)
    private String status;
    @Schema(accessMode = READ_ONLY)
    private RefundSettlementSummary settlementSummary;

    private RefundResponse(RefundFromConnector refund, URI selfLink, URI paymentLink) {
        this.refundId = refund.getRefundId();
        this.amount = refund.getAmount();
        this.status = refund.getStatus();
        this.createdDate = refund.getCreatedDate();
        this.links = new RefundLinksForSearch();
        this.settlementSummary = new RefundSettlementSummary();

        links.addSelf(selfLink.toString());
        links.addPayment(paymentLink.toString());
    }

    private RefundResponse(RefundTransactionFromLedger refund, URI selfLink, URI paymentLink) {
        this.refundId = refund.getTransactionId();
        this.amount = refund.getAmount();
        this.status = refund.getState().getStatus();
        this.createdDate = refund.getCreatedDate();
        this.settlementSummary = refund.getSettlementSummary();
        this.links = new RefundLinksForSearch();

        links.addSelf(selfLink.toString());
        links.addPayment(paymentLink.toString());
    }

    private RefundResponse(String refundId, Long amount, String status,
                           String createdDate, URI selfLink, URI paymentLink) {
        this.refundId = refundId;
        this.amount = amount;
        this.status = status;
        this.createdDate = createdDate;
        this.links = new RefundLinksForSearch();

        links.addSelf(selfLink.toString());
        links.addPayment(paymentLink.toString());
    }

    public static RefundResponse from(RefundFromConnector refund, URI selfLink, URI paymentLink) {
        return new RefundResponse(refund, selfLink, paymentLink);
    }

    public static RefundResponse from(RefundTransactionFromLedger refund, URI selfLink, URI paymentLink) {
        return new RefundResponse(refund, selfLink, paymentLink);
    }

    //todo: remove after full refactoring of PaymentRefundsResource (to use service layer) 
    public static RefundResponse valueOf(RefundFromConnector refundEntity, String paymentId, String baseUrl) {
        URI selfLink = UriBuilder.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}/refunds/{refundId}")
                .build(paymentId, refundEntity.getRefundId());

        URI paymentLink = UriBuilder.fromUri(baseUrl)
                .path("/v1/payments/{paymentId}")
                .build(paymentId);

        return new RefundResponse(
                refundEntity.getRefundId(),
                refundEntity.getAmount(),
                refundEntity.getStatus(),
                refundEntity.getCreatedDate(),
                selfLink,
                paymentLink);
    }

    public String getRefundId() {
        return refundId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public RefundLinksForSearch getLinks() {
        return links;
    }

    public RefundSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }
}
