package uk.gov.pay.api.it.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.api.model.links.Link;

import java.util.List;

public class PaymentRefundJsonFixture {

    private Long amount;

    @JsonProperty(value = "created_date")
    private String createdDate;

    @JsonProperty(value = "refund_id")
    private String refundId;

    private String status;

    @JsonProperty(value = "_links")
    private List<Link> links;

    public PaymentRefundJsonFixture() {}

    public PaymentRefundJsonFixture(Long amount, String createdDate, String refundId, String status, List<Link> links) {
        this.amount = amount;
        this.createdDate = createdDate;
        this.refundId = refundId;
        this.status = status;
        this.links = links;
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
}
