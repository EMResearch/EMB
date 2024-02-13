package uk.gov.pay.api.model.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "SettlementSummary", description = "Contains information about when a lost dispute was settled. A dispute is settled when your payment service provider takes it from a payout to your bank account. 'settlement_summary' only appears if you lost the dispute.")
public class DisputeSettlementSummary {

    @JsonProperty("settled_date")
    private String settledDate;

    public DisputeSettlementSummary() {}

    public DisputeSettlementSummary(String settledDate) {
        this.settledDate = settledDate;
    }

    @Schema(description = "The date your payment service provider took the disputed payment " +
            "and dispute fee from a payout to your bank account. " +
            "This value appears in ISO 8601 format - `YYYY-MM-DD`. " +
            "`settled_date` only appears if you lost the dispute.", 
            example = "2022-07-28",
            accessMode = READ_ONLY)
    public String getSettledDate() {
        return settledDate;
    }
}
