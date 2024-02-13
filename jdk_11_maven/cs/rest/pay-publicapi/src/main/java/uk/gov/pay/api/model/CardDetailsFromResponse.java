package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

@JsonInclude(ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardDetailsFromResponse {

    @JsonProperty("last_digits_card_number")
    private final String lastDigitsCardNumber;

    @JsonProperty("first_digits_card_number")
    private final String firstDigitsCardNumber;

    @JsonProperty("cardholder_name")
    private final String cardHolderName;

    @JsonProperty("expiry_date")
    private final String expiryDate;

    @JsonProperty("billing_address")
    private final Address billingAddress;

    @JsonProperty("card_brand")
    private final String cardBrand;
    
    @JsonProperty("card_type")
    private final String cardType;

    public CardDetailsFromResponse(@JsonProperty("last_digits_card_number") String lastDigitsCardNumber,
                                   @JsonProperty("first_digits_card_number") String firstDigitsCardNumber,
                                   @JsonProperty("cardholder_name") String cardHolderName,
                                   @JsonProperty("expiry_date") String expiryDate,
                                   @JsonProperty("billing_address") Address billingAddress,
                                   @JsonProperty("card_brand") String cardBrand,
                                   @JsonProperty("card_type") String cardType) {
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        this.cardHolderName = cardHolderName;
        this.expiryDate = expiryDate;
        this.billingAddress = billingAddress;
        this.cardBrand = cardBrand;
        this.cardType = cardType;
    }

    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public Optional<Address> getBillingAddress() {
        return Optional.ofNullable(billingAddress);
    }

    public String getCardBrand() {
        return cardBrand;
    }


    public String getCardType() {
        return cardType;
    }
}
