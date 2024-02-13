package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(name = "RefundSummary", description = "A structure representing the refunds availability")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundSummary {

    private String status;

    @JsonProperty("amount_available")
    private long amountAvailable;

    @JsonProperty("amount_submitted")
    private long amountSubmitted;

    public RefundSummary() {}

    public RefundSummary(String status, long amountAvailable, long amountSubmitted) {
        this.status = status;
        this.amountAvailable = amountAvailable;
        this.amountSubmitted = amountSubmitted;
    }

    @Schema(description = "Whether you can [refund the payment]" +
            "(https://docs.payments.service.gov.uk/refunding_payments/#checking-the-status-of-a-refund-status).", 
            example = "available")
    public String getStatus() {
        return status;
    }

    @Schema(description = "How much you can refund to the user, in pence.", example = "100", accessMode = READ_ONLY)
    public long getAmountAvailable() {
        return amountAvailable;
    }

    @Schema(description = "How much you’ve already refunded to the user, in pence.", accessMode = READ_ONLY)
    public long getAmountSubmitted() {
        return amountSubmitted;
    }
}
