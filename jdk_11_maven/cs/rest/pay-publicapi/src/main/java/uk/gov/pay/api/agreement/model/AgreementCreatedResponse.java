package uk.gov.pay.api.agreement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgreementCreatedResponse {

    @JsonProperty("agreement_id")
    private String agreementId;

    public AgreementCreatedResponse() {
    }

    public AgreementCreatedResponse(String agreementId){
        this.agreementId = agreementId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    @Override
    public String toString() {
        return "AgreementCreatedResponse{" +
                "agreementId='" + agreementId +'}';
    }
}
