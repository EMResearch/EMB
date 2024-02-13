package uk.gov.pay.api.model.search.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.RefundSettlementSummary;
import uk.gov.pay.api.model.ledger.RefundTransactionFromLedger;
import uk.gov.pay.api.model.links.RefundLinksForSearch;

import java.net.URI;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "RefundDetailForSearch")
public class RefundForSearchRefundsResult {

    @JsonProperty("refund_id")
    @Schema(example = "act4c33g40j3edfmi8jknab84x", 
            description = "The unique ID GOV.UK Pay automatically associated with this refund when you created it.", 
            accessMode = READ_ONLY)
    private String refundId;

    @JsonProperty("created_date")
    @Schema(example = "2017-01-10T16:52:07.855Z",
            description = "The date and time you created this refund. " +
                    "This value uses Coordinated Universal Time (UTC) and ISO 8601 format - `YYYY-MM-DDThh:mm:ss.SSSZ`.",
            accessMode = READ_ONLY)
    private String createdDate;

    private String chargeId;

    private Long amount;

    private RefundLinksForSearch links = new RefundLinksForSearch();

    @JsonProperty("status")
    @Schema(example = "success", 
            description = "The [status of the refund]" +
                    "(https://docs.payments.service.gov.uk/refunding_payments/#checking-the-status-of-a-refund-status).",
            allowableValues = {"submitted", "success", "error"}, accessMode = READ_ONLY)
    private String status;

    @Schema(accessMode = READ_ONLY)
    private RefundSettlementSummary settlementSummary;

    public RefundForSearchRefundsResult() {
    }

    public RefundForSearchRefundsResult(String refundId, String createdDate, String status,
                                        String chargeId, Long amount, URI paymentURI, URI refundsURI,
                                        RefundSettlementSummary settlementSummary) {
        this.refundId = refundId;
        this.createdDate = createdDate;
        this.status = status;
        this.chargeId = chargeId;
        this.amount = amount;
        this.links.addSelf(refundsURI.toString());
        this.links.addPayment(paymentURI.toString());
        this.settlementSummary = settlementSummary;
    }

    public String getRefundId() {
        return refundId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getStatus() {
        return status;
    }

    @JsonProperty("payment_id")
    @Schema(hidden = true)
    public String getChargeId() {
        return chargeId;
    }

    @JsonProperty("charge_id")
    @Schema(hidden = true)
    public void setChargeId(String chargeId) {
        this.chargeId = chargeId;
    }

    @JsonProperty("amount_submitted")
    @Schema(hidden = true)
    public void setAmount(Long amount) {
        this.amount = amount;
    }

    @JsonProperty("amount")
    @Schema(example = "120", description = "The amount refunded to the user in pence.", accessMode = READ_ONLY)
    public Long getAmount() {
        return amount;
    }

    @JsonProperty("_links")
    public RefundLinksForSearch getLinks() {
        return links;
    }

    @JsonProperty("settlement_summary")
    @Schema(accessMode = READ_ONLY)
    public RefundSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public static RefundForSearchRefundsResult valueOf(RefundTransactionFromLedger refundResult, URI paymentURI, URI refundsURI) {
        return new RefundForSearchRefundsResult(
                refundResult.getTransactionId(),
                refundResult.getCreatedDate(),
                refundResult.getState().getStatus(),
                refundResult.getParentTransactionId(),
                refundResult.getAmount(),
                paymentURI,
                refundsURI,
                refundResult.getSettlementSummary());
    }

    @Override
    public String toString() {
        return "RefundForSearchRefundsResult{" +
                "refundId='" + refundId + '\'' +
                ", createdDate='" + createdDate + '\'' +
                ", status='" + status + '\'' +
                ", amount=" + amount + '\'' +
                ", links=" + links + '\'' +
                '}';
    }
}
