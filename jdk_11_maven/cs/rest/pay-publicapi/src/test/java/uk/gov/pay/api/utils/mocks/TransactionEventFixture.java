package uk.gov.pay.api.utils.mocks;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.PaymentState;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionEventFixture {
    private PaymentState state;
    private String timestamp;

    public PaymentState getState() {
        return state;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public TransactionEventFixture(TransactionEventFixtureBuilder builder) {
        this.state = builder.state;
        this.timestamp = builder.timestamp;
    }

    public static final class TransactionEventFixtureBuilder {
        private PaymentState state;
        private String timestamp;
        
        public TransactionEventFixtureBuilder withState(PaymentState state) {
            this.state = state;
            return this;
        }
        
        public TransactionEventFixtureBuilder withTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public static TransactionEventFixtureBuilder aTransactionEventFixture() {
            return new TransactionEventFixtureBuilder();
        }
        
        public TransactionEventFixture build() {
            return new TransactionEventFixture(this);
        }
    }
}
