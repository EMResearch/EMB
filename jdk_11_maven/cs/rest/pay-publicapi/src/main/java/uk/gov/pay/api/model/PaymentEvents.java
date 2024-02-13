package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvents {
    @JsonProperty("charge_id")
    private String chargeId;

    private List<PaymentEvent> events;

    public PaymentEvents() {}

    public String getChargeId() {
        return chargeId;
    }

    public List<PaymentEvent> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        return "PaymentEvents{" +
                "chargeId='" + chargeId + '\'' +
                ", events=" + events +
                "}";
    }
}
