package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionEvents {

    private String transactionId;
    private List<TransactionEvent> events;

    public TransactionEvents() {}

    public String getTransactionId() {
        return transactionId;
    }

    public List<TransactionEvent> getEvents() {
        return events;
    }
}
