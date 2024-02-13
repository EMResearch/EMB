package uk.gov.pay.api.model.telephone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.api.validation.ValidPaymentOutcome;

import java.util.Optional;

@ValidPaymentOutcome(message = "Field [payment_outcome] must include a valid status and error code")
public class PaymentOutcome {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("supplemental")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private Supplemental supplemental;

    public PaymentOutcome() {
    }
    
    public PaymentOutcome(String status) {
        this.status = status;
    }
    
    public PaymentOutcome(String status, String code, Supplemental supplemental) {
        // For testing deserialization
        this.status = status;
        this.code = code;
        this.supplemental = supplemental;
    }

    public String getStatus() {
        return status;
    }

    @JsonIgnore
    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }
    
    @JsonIgnore
    public Optional<Supplemental> getSupplemental() {
        return Optional.ofNullable(supplemental);
    }
}
