package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "RefundSettlementSummary", description = "A structure representing information about a settlement for refunds")
public class RefundSettlementSummary {

    @JsonProperty("settled_date")
    private String settledDate;

    public RefundSettlementSummary() {}

    public RefundSettlementSummary(String settledDate) {
        this.settledDate = settledDate;
    }

    @Schema(description = "The date Stripe took the refund from a payout to your bank account. " +
            "`settled_date` only appears if Stripe has taken the refund. " +
            "This value uses Coordinated Universal Time (UTC) and ISO 8601 format - `YYYY-MM-DD`.", 
            example = "2016-01-21",
            accessMode = READ_ONLY)
    public String getSettledDate() {
        return settledDate;
    }
}
