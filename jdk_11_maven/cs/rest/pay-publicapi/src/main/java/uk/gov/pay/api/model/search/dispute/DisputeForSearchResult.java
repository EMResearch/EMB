package uk.gov.pay.api.model.search.dispute;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.ledger.DisputeSettlementSummary;
import uk.gov.pay.api.model.ledger.DisputeTransactionFromLedger;
import uk.gov.pay.api.model.links.DisputeLinksForSearch;

import java.net.URI;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static uk.gov.pay.api.model.CardPayment.LINKS_JSON_ATTRIBUTE;

@Schema(name = "DisputeDetailForSearch")
public class DisputeForSearchResult {
    private Long amount;
    @JsonProperty("created_date")
    private String createdDate;
    @JsonProperty("dispute_id")
    private String disputeId;
    @JsonProperty("evidence_due_date")
    private String evidenceDueDate;
    private Long fee;
    @JsonProperty("net_amount")
    private Long netAmount;
    @JsonProperty("payment_id")
    private String paymentId;
    private String reason;
    @JsonProperty("settlement_summary")
    private DisputeSettlementSummary settlementSummary;
    private String status;
    @JsonProperty(LINKS_JSON_ATTRIBUTE)
    private DisputeLinksForSearch links = new DisputeLinksForSearch();

    public DisputeForSearchResult(Long amount, String createdDate, String disputeId, String evidenceDueDate,
                                  Long fee, Long netAmount, String paymentId, String reason,
                                  DisputeSettlementSummary settlementSummary, String status, URI paymentURI) {
        this.amount = amount;
        this.createdDate = createdDate;
        this.disputeId = disputeId;
        this.evidenceDueDate = evidenceDueDate;
        this.fee = fee;
        this.netAmount = netAmount;
        this.paymentId = paymentId;
        this.reason = reason;
        this.settlementSummary = settlementSummary;
        this.status = status;
        this.links.addPayment(paymentURI.toString());
    }

    public static DisputeForSearchResult valueOf(DisputeTransactionFromLedger fromLedger, URI paymentUri) {
        return new DisputeForSearchResult(fromLedger.getAmount(), fromLedger.getCreatedDate(), fromLedger.getTransactionId(),
                fromLedger.getEvidenceDueDate(), fromLedger.getFee(), fromLedger.getNetAmount(), fromLedger.getParentTransactionId(),
                fromLedger.getReason(), fromLedger.getSettlementSummary(), fromLedger.getState().getStatus(),
                paymentUri);
    }

    @Schema(example = "1200", description = "The disputed amount in pence.", accessMode = READ_ONLY)
    public Long getAmount() {
        return amount;
    }

    @Schema(example = "2022-07-28T16:43:00.000Z", description = "The date and time the user's bank told GOV.UK Pay about this dispute.", accessMode = READ_ONLY)
    public String getCreatedDate() {
        return createdDate;
    }

    @Schema(example = "hu20sqlact5260q2nanm0q8u93", description = "The unique ID GOV.UK Pay automatically associated with this dispute when the paying user disputed the payment.", accessMode = READ_ONLY)
    public String getDisputeId() {
        return disputeId;
    }

    @Schema(example = "2022-07-28T16:43:00.000Z", description = "The deadline for submitting your supporting evidence. This value uses Coordinated Universal Time (UTC) and ISO 8601 format", accessMode = READ_ONLY)
    public String getEvidenceDueDate() {
        return evidenceDueDate;
    }

    @Schema(example = "1200", description = "The payment service provider’s dispute fee, in pence.", accessMode = READ_ONLY)
    public Long getFee() {
        return fee;
    }

    @Schema(example = "-2400", description = "The amount, in pence, your payment service provider will take for a lost dispute. 'net_amount' is deducted from your payout after you lose the dispute. For example, a 'net_amount' of '-1500' means your PSP will take £15.00 from your next payout into your bank account. 'net_amount' is always a negative value. 'net_amount' only appears if you lose the dispute.", accessMode = READ_ONLY)
    public Long getNetAmount() {
        return netAmount;
    }

    @Schema(example = "hu20sqlact5260q2nanm0q8u93", description = "The unique ID GOV.UK Pay automatically associated with this payment when you created it.", accessMode = READ_ONLY)
    public String getPaymentId() {
        return paymentId;
    }

    @Schema(example = "fraudulent", description = "The reason the paying user gave for disputing this payment. Possible values are: 'credit_not_processed', 'duplicate', 'fraudulent', 'general', 'product_not_received', 'product_unacceptable', 'unrecognised', 'subscription_cancelled', >'other'", accessMode = READ_ONLY)
    public String getReason() {
        return reason;
    }

    public DisputeSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    @Schema(example = "under_review", description = "The current status of the dispute. Possible values are: 'needs_response', 'won', 'lost', 'under_review'", accessMode = READ_ONLY)
    public String getStatus() {
        return status;
    }

    @Schema(description = "Contains an API method and endpoint to get information about this payment. A 'GET' request ('method') to this endpoint ('href') returns information about this payment.")
    public DisputeLinksForSearch getLinks() {
        return links;
    }
}
