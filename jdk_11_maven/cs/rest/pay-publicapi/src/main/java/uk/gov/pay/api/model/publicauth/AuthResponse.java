package uk.gov.pay.api.model.publicauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.TokenPaymentType;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {

    private String accountId;
    private String tokenLink;
    private TokenPaymentType tokenType;

    public AuthResponse() {
    }

    public AuthResponse(String accountId, String tokenLink, TokenPaymentType tokenType) {
        this.accountId = accountId;
        this.tokenLink = tokenLink;
        this.tokenType = tokenType;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getTokenLink() {
        return tokenLink;
    }

    public TokenPaymentType getTokenType() {
        return tokenType;
    }
}
