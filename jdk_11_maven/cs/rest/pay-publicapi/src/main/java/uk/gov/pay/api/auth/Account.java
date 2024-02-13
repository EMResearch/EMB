package uk.gov.pay.api.auth;

import uk.gov.pay.api.model.TokenPaymentType;

import java.security.Principal;

public class Account implements Principal {

    private final String accountId;
    private final TokenPaymentType paymentType;
    private final String tokenLink;

    public Account(String accountId, TokenPaymentType paymentType, String tokenLink) {
        this.accountId = accountId;
        this.paymentType = paymentType;
        this.tokenLink = tokenLink;
    }

    @Override
    public String getName() {
        return getAccountId();
    }

    public String getTokenLink() {
        return tokenLink;
    }

    public String getAccountId() {
        return accountId;
    }

    public TokenPaymentType getPaymentType() {
        return paymentType;
    }
}
