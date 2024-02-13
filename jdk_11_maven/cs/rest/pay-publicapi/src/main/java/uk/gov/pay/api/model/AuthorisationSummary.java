package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthorisationSummary", description = "Object containing information about the authentication of the payment.")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorisationSummary {

    @JsonProperty("three_d_secure")
    private ThreeDSecure threeDSecure;

    public AuthorisationSummary() {
    }

    public AuthorisationSummary(ThreeDSecure threeDSecure) {
        this.threeDSecure = threeDSecure;
    }

    public ThreeDSecure getThreeDSecure() {
        return threeDSecure;
    }
}
