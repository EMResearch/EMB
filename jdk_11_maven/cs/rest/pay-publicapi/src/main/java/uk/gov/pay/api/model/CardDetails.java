package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@JsonInclude(ALWAYS)
@Schema(name = "CardDetails", description = "A structure representing the payment card")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardDetails {

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
    @JsonProperty("wallet_type")
    private String walletType;

    public CardDetails(String lastDigitsCardNumber,
                       String firstDigitsCardNumber,
                       String cardHolderName,
                       String expiryDate,
                       Address billingAddress,
                       String cardBrand,
                       String cardType,
                       String walletType
    ) {
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        this.cardHolderName = cardHolderName;
        this.expiryDate = expiryDate;
        this.billingAddress = billingAddress;
        this.cardBrand = cardBrand;
        this.cardType = cardType;
        this.walletType = walletType;
    }

    public static CardDetails from(CardDetailsFromResponse cardDetailsFromResponse, String walletType) {
        if (cardDetailsFromResponse == null) {
            return null;
        } else {
            return new CardDetails(
                    cardDetailsFromResponse.getLastDigitsCardNumber(),
                    cardDetailsFromResponse.getFirstDigitsCardNumber(),
                    cardDetailsFromResponse.getCardHolderName(),
                    cardDetailsFromResponse.getExpiryDate(),
                    cardDetailsFromResponse.getBillingAddress().orElse(null),
                    cardDetailsFromResponse.getCardBrand(),
                    cardDetailsFromResponse.getCardType(),
                    walletType
            );
        }
        
    }


    @Schema(example = "1234", accessMode = READ_ONLY)
    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    @Schema(example = "123456", accessMode = READ_ONLY)
    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    @Schema(example = "Mr. Card holder")
    public String getCardHolderName() {
        return cardHolderName;
    }

    @Schema(description = "The expiry date of the card the user paid with in `MM/YY` format.", example = "04/24", accessMode = READ_ONLY)
    public String getExpiryDate() {
        return expiryDate;
    }

    public Optional<Address> getBillingAddress() {
        return Optional.ofNullable(billingAddress);
    }

    @Schema(example = "Visa", description = "The brand of card the user paid with.", accessMode = READ_ONLY)
    public String getCardBrand() {
        return cardBrand;
    }

    @Schema(description = "The type of card the user paid with." +
            "`null` means your user paid with Google Pay or " +
            "we did not recognise which type of card they paid with.",
            allowableValues = {"debit", "credit", "null"}, example = "debit", accessMode = READ_ONLY)
    public String getCardType() {
        return cardType;
    }

    public void setWalletType(String walletType) {
        this.walletType = walletType;
    }

    @Schema(example = "Apple Pay", description = "The digital wallet type that the user paid with", allowableValues = {"Apple Pay", "Google Pay"})
    public Optional<String> getWalletType() {
        return Optional.ofNullable(walletType);
    }
}
