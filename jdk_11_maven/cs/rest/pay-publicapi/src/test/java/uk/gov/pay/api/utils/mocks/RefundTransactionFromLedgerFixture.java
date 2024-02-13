package uk.gov.pay.api.utils.mocks;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.RefundSettlementSummary;
import uk.gov.pay.api.model.ledger.TransactionState;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RefundTransactionFromLedgerFixture {
    private final Long amount;
    private final TransactionState state;
    private final String createdDate;
    private final String transactionId;
    private final String parentTransactionId;
    private final RefundSettlementSummary settlementSummary;

    public RefundTransactionFromLedgerFixture(RefundTransactionFromLedgerBuilder builder) {
        this.amount = builder.amount;
        this.state = builder.state;
        this.createdDate = builder.createdDate;
        this.transactionId = builder.transactionId;
        this.parentTransactionId = builder.parentTransactionId;
        this.settlementSummary = builder.settlementSummary;
    }

    public Long getAmount() {
        return amount;
    }

    public TransactionState getState() {
        return state;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public RefundSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public static final class RefundTransactionFromLedgerBuilder {
        private Long amount;
        private TransactionState state;
        private String createdDate;
        private String transactionId;
        private String parentTransactionId;
        private RefundSettlementSummary settlementSummary = new RefundSettlementSummary();

        private RefundTransactionFromLedgerBuilder() {
        }

        public static RefundTransactionFromLedgerBuilder aRefundTransactionFromLedgerFixture() {
            return new RefundTransactionFromLedgerBuilder();
        }

        public RefundTransactionFromLedgerBuilder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public RefundTransactionFromLedgerBuilder withState(TransactionState state) {
            this.state = state;
            return this;
        }

        public RefundTransactionFromLedgerBuilder withCreatedDate(String createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public RefundTransactionFromLedgerBuilder withTransactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public RefundTransactionFromLedgerBuilder withParentTransactionId(String parentTransactionId) {
            this.parentTransactionId = parentTransactionId;
            return this;
        }

        public RefundTransactionFromLedgerFixture build() {
            return new RefundTransactionFromLedgerFixture(this);
        }

        public RefundTransactionFromLedgerBuilder withSettlementSummary(String settledDate) {
            this.settlementSummary = new RefundSettlementSummary(settledDate);
            return this;
        }
    }
}
