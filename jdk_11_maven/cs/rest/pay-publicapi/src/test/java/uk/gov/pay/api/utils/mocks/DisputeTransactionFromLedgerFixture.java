package uk.gov.pay.api.utils.mocks;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.api.model.ledger.DisputeSettlementSummary;
import uk.gov.pay.api.model.ledger.TransactionState;

public class DisputeTransactionFromLedgerFixture {
    @JsonProperty("amount")
    private Long amount;
    @JsonProperty("created_date")
    private String createdDate;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("evidence_due_date")
    private String evidenceDueDate;
    @JsonProperty("fee")
    private Long fee;
    @JsonProperty("net_amount")
    private Long netAmount;
    @JsonProperty("parent_transaction_id")
    private String parentTransactionId;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("settlement_summary")
    private DisputeSettlementSummary settlementSummary;
    @JsonProperty("state")
    private TransactionState state;
    @JsonProperty("transaction_type")
    private String transActionType = "DISPUTE";

    public DisputeTransactionFromLedgerFixture(DisputeTransactionFromLedgerBuilder builder) {
        this.amount = builder.amount;
        this.createdDate = builder.createdDate;
        this.transactionId = builder.transactionId;
        this.evidenceDueDate = builder.evidenceDueDate;
        this.fee = builder.fee;
        this.netAmount = builder.netAmount;
        this.parentTransactionId = builder.parentTransactionId;
        this.reason = builder.reason;
        this.settlementSummary = builder.settlementSummary;
        this.state = builder.state;
    }

    public Long getAmount() {
        return amount;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getEvidenceDueDate() {
        return evidenceDueDate;
    }

    public Long getFee() {
        return fee;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public String getReason() {
        return reason;
    }

    public DisputeSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public TransactionState getState() {
        return state;
    }

    public String getTransActionType() {
        return transActionType;
    }

    public static final class DisputeTransactionFromLedgerBuilder {
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

        private DisputeTransactionFromLedgerBuilder() {
        }

        public static DisputeTransactionFromLedgerBuilder aDisputeTransactionFromLedgerFixture() {
            return new DisputeTransactionFromLedgerBuilder();
        }

        public DisputeTransactionFromLedgerBuilder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withCreatedDate(String createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withTransactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withEvidenceDueDate(String evidenceDueDate) {
            this.evidenceDueDate = evidenceDueDate;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withFee(Long fee) {
            this.fee = fee;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withNetAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withParentTransactionId(String parentTransactionId) {
            this.parentTransactionId = parentTransactionId;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withReason(String reason) {
            this.reason = reason;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withSettlementSummary(DisputeSettlementSummary settlementSummary) {
            this.settlementSummary = settlementSummary;
            return this;
        }

        public DisputeTransactionFromLedgerBuilder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public DisputeTransactionFromLedgerFixture build() {
            return new DisputeTransactionFromLedgerFixture(this);
        }
    }
}
