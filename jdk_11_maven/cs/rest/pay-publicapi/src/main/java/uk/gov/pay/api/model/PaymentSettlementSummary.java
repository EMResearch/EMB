package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "PaymentSettlementSummary", description = "A structure representing information about a settlement")
public class PaymentSettlementSummary {

    @JsonProperty("capture_submit_time")
    private String captureSubmitTime;

    @JsonProperty("captured_date")
    private String capturedDate;

    @JsonProperty("settled_date")
    private String settledDate;

    public PaymentSettlementSummary() {}

    public PaymentSettlementSummary(String captureSubmitTime, String capturedDate, String settledDate) {
        this.captureSubmitTime = captureSubmitTime;
        this.capturedDate = capturedDate;
        this.settledDate = settledDate;
    }

    @Schema(description = "The date and time GOV.UK Pay asked your payment service provider " +
            "to take the payment from your user’s account. " +
            "This value uses Coordinated Universal Time (UTC) and ISO 8601 format - `YYYY-MM-DDThh:mm:ss.SSSZ`",
            example = "2016-01-21T17:15:00.000Z", accessMode = READ_ONLY)
    public String getCaptureSubmitTime() {
        return captureSubmitTime;
    }

    @Schema(description = "The date your payment service provider took the payment from your user. " +
            "This value uses ISO 8601 format - `YYYY-MM-DD`", 
            example = "2016-01-21", accessMode = READ_ONLY)
    public String getCapturedDate() {
        return capturedDate;
    }

    @Schema(description = "The date that the transaction was paid into the service's account.", example = "2016-01-21",
            accessMode = READ_ONLY)
    public String getSettledDate() {
        return settledDate;
    }
}
