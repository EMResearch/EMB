package uk.gov.pay.api.it.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.links.Link;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PaymentRefundSearchJsonFixture {

    @JsonProperty(value = "amount_submitted")
    private Long amount;

    @JsonProperty(value = "created_date")
    private String createdDate;

    @JsonProperty(value = "refund_id")
    private String refundId;

    @JsonProperty(value = "charge_id")
    private String chargeId;

    private String status;

    @JsonProperty(value = "links")
    private List<Link> links;

    public PaymentRefundSearchJsonFixture() {
    }

    public PaymentRefundSearchJsonFixture(Long amount, String createdDate, String refundId, String chargeId, String status, List<Link> links) {
        this.amount = amount;
        this.createdDate = createdDate;
        this.refundId = refundId;
        this.status = status;
        this.links = links;
        this.chargeId = chargeId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getRefundId() {
        return refundId;
    }

    public String getStatus() {
        return status;
    }

    public List<Link> getLinks() {
        return links;
    }

    public String getChargeId() {
        return chargeId;
    }
}
