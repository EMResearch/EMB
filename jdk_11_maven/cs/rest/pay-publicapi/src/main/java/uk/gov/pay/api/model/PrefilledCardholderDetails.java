package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrefilledCardholderDetails {

    @JsonProperty("cardholder_name")
    @Schema(name = "cardholder_name", 
            description = "The cardholder name you prefilled when you created this payment.", 
            example = "J. Bogs")
    @Size(max = 255, message = "Must be less than or equal to {max} characters length")
    private String cardholderName;

    @Schema(name = "billing_address", description = "prefilled billing address")
    @JsonProperty("billing_address")
    @Valid
    private Address billingAddress;

    public Optional<Address> getBillingAddress() {
        return Optional.ofNullable(billingAddress);
    }
    
    public Optional<String> getCardholderName() {
        return Optional.ofNullable(cardholderName);
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }
    
    public void setAddress(String addressLine1, String addressLine2, String postcode, String city, String country) {
        this.billingAddress = new Address(addressLine1, addressLine2, postcode, city, country);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrefilledCardholderDetails that = (PrefilledCardholderDetails) o;
        return Objects.equals(cardholderName, that.cardholderName) &&
                Objects.equals(billingAddress, that.billingAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardholderName, billingAddress);
    }
}
