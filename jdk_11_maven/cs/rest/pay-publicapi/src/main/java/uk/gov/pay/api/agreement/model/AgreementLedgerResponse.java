package uk.gov.pay.api.agreement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.CardDetailsFromResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgreementLedgerResponse {
    private String externalId;
    private String serviceId;
    private String reference;
    private String description;
    private String status;
    private String createdDate;
    private PaymentInstrumentLedgerResponse paymentInstrument;
    private String userIdentifier;
    private String cancelledDate;

    @JsonProperty("external_id")
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @JsonProperty("id")
    public String getExternalId() {
        return externalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public PaymentInstrumentLedgerResponse getPaymentInstrument() {
        return paymentInstrument;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public String getCancelledDate() {
        return cancelledDate;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PaymentInstrumentLedgerResponse {

        private String externalId;
        private String agreementExternalId;
        private CardDetailsFromResponse cardDetails;
        private String createdDate;
        private String type;

        public PaymentInstrumentLedgerResponse() {
            // Janet Jackson
        }
        
        private PaymentInstrumentLedgerResponse(Builder builder) {
            this.externalId = builder.externalId;
            this.agreementExternalId = builder.agreementExternalId;
            this.cardDetails = builder.cardDetails;
            this.createdDate = builder.createdDate;
            this.type = builder.type;
        }

        public String getExternalId() {
            return externalId;
        }

        public String getAgreementExternalId() {
            return agreementExternalId;
        }

        public CardDetailsFromResponse getCardDetails() {
            return cardDetails;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public String getType() {
            return type;
        }

        public static class Builder {
            private String externalId;
            private String agreementExternalId;
            private CardDetailsFromResponse cardDetails;
            private String createdDate;
            private String type;

            public PaymentInstrumentLedgerResponse build() {
                return new PaymentInstrumentLedgerResponse(this);
            }

            public Builder withExternalId(String externalId) {
                this.externalId = externalId;
                return this;
            }

            public Builder withAgreementExternalId(String agreementExternalId) {
                this.agreementExternalId = agreementExternalId;
                return this;
            }

            public Builder withCardDetails(CardDetailsFromResponse cardDetails) {
                this.cardDetails = cardDetails;
                return this;
            }

            public Builder withCreatedDate(String createdDate) {
                this.createdDate = createdDate;
                return this;
            }

            public Builder withType(String type) {
                this.type = type;
                return this;
            }
        }
    }
}
