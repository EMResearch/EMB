package uk.gov.pay.api.model;

public enum Wallet {

    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay");

    private final String titleCase;

    Wallet(String titleCase) {
        this.titleCase = titleCase;
    }

    public String getTitleCase() {
        return titleCase;
    }
}
