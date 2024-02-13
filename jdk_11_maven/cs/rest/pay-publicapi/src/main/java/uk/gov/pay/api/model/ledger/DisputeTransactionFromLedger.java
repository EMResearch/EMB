package uk.gov.pay.api.model.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisputeTransactionFromLedger {
    private Long amount;
    private String createdDate;
    private String transactionId;
    private String evidenceDueDate;
    private Long fee;
    private Long netAmount;
    private String parentTransactionId;
    private String reason;
    private DisputeSettlementSummary settlementSummary;
    private TransactionState state;

    public Long getAmount() {
        return amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionState getState() {
        return state;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public Long getFee() {
        return fee;
    }

    public String getReason() {
        return reason;
    }

    public DisputeSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getEvidenceDueDate() {
        return evidenceDueDate;
    }
}
