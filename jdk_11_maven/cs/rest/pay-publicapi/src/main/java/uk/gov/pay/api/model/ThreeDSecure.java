package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ThreeDSecure", description = "Object containing information about the 3D Secure authentication of the payment.")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreeDSecure {

    @JsonProperty("required")
    @Schema(name = "required", description = "Indicates if this payment was authorised with 3D Secure authentication. " +
            "`required` is `true` if the payment required 3D Secure authentication.")
    private boolean required;

    public ThreeDSecure() {
    }

    public ThreeDSecure(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

}
