package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent {

    @JsonProperty("state")
    private PaymentState state;

    @JsonProperty("updated")
    private String updated;

    public PaymentEvent() {}

    public PaymentState getState() {
        return state;
    }

    public String getUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "state='" + state + '\'' +
                ", updated=" + updated +
                "}";
    }
}
