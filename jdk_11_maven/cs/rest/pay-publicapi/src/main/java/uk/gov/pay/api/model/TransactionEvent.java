package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEvent {

    private PaymentState state;
    private String timestamp;

    public TransactionEvent() {}

    public PaymentState getState() {
        return state;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
