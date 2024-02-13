package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
@Schema(name = "AuthorisationRequest", description = "Contains the user's payment information. This information will be sent to the payment service provider to authorise the payment.")
public class AuthorisationRequest {
    @JsonProperty("one_time_token")
    @NotBlank
    private String oneTimeToken;
    @JsonProperty("card_number")
    @NotNull
    @Size(min = 12, max = 19, message = "Must be between 12 and 19 characters long")
    private String cardNumber;
    @JsonProperty("cvc")
    @NotNull
    @Size(min = 3, max = 4, message = "Must be between 3 and 4 characters long")
    private String cvc;
    @JsonProperty("expiry_date")
    @NotNull
    @Size(min = 5, max = 5, message = "Must be a valid date with the format MM/YY")
    private String expiryDate;
    @JsonProperty("cardholder_name")
    @NotBlank
    @Size(max = 255, message = "Must be less than or equal to {max} characters long")
    private String cardholderName;

    public AuthorisationRequest() {
    }

    public AuthorisationRequest(String oneTimeToken, String cardNumber, String cvc, String expiryDate, String cardholderName) {
        this.oneTimeToken = oneTimeToken;
        this.cardNumber = cardNumber;
        this.cvc = cvc;
        this.expiryDate = expiryDate;
        this.cardholderName = cardholderName;
    }

    @Schema(description = "This single use token authorises your request and matches it to a payment. GOV.UK Pay generated the `one_time_token` when the payment was created.", required = true, example = "12345-edsfr-6789-gtyu")
    public String getOneTimeToken() {
        return oneTimeToken;
    }

    @Schema(description = "The full card number from the paying user's card.", required = true, minLength = 12, maxLength = 19, example = "4242424242424242")
    public String getCardNumber() {
        return cardNumber;
    }

    @Schema(description = "The card verification code (CVC) or card verification value (CVV) on the paying user's card.", required = true, minLength = 3, maxLength = 4, example = "123")
    public String getCvc() {
        return cvc;
    }

    @Schema(description = "The expiry date of the paying user's card. This value must be in `MM/YY` format.", required = true, minLength = 5, maxLength = 5, example = "09/22")
    public String getExpiryDate() {
        return expiryDate;
    }

    @Schema(description = "The name on the paying user's card.", required = true, maxLength = 255, example = "J. Citizen")
    public String getCardholderName() {
        return cardholderName;
    }
}
