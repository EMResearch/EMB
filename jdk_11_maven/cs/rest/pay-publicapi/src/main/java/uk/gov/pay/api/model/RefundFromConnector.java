package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundFromConnector {

    @JsonProperty(value = "refund_id")
    private String refundId;

    @JsonProperty(value = "created_date")
    private String createdDate;

    private Long amount;
    private String status;

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

    @Override
    public String toString() {
        return "RefundFromConnector{" +
                "refundId='" + refundId + '\'' +
                ", createdDate='" + createdDate + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
    }
}
