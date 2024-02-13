package uk.gov.pay.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.api.model.PaymentState;

import java.util.Map;

public class ChargeEventBuilder {
    @JsonDeserialize
    @JsonSerialize
    private PaymentState state;

    @JsonDeserialize
    @JsonSerialize
    private String updated;

    public ChargeEventBuilder(PaymentState state, String updated) {
        this.state = state;
        this.updated = updated;
    }

    public Map build() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(this, Map.class);
    }

}
