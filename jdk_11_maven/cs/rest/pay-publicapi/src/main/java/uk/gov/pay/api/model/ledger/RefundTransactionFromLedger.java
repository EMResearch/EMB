package uk.gov.pay.api.model.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.RefundSettlementSummary;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundTransactionFromLedger {

    Long amount;
    String description;
    String reference;
    String createdDate;
    String refundedBy;
    String transactionId;
    String parentTransactionId;
    TransactionState state;
    RefundSettlementSummary settlementSummary;
    public Long getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getRefundedBy() {
        return refundedBy;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public TransactionState getState() {
        return state;
    }

    public RefundSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }
}
