package uk.gov.pay.api.model;

import uk.gov.pay.api.agreement.model.CreateAgreementRequest;

public class CreateAgreementRequestBuilder {
    private String reference;
    private String description;
    private String userIdentifier;
    public static CreateAgreementRequestBuilder builder() {
        return new CreateAgreementRequestBuilder();
    }

    public CreateAgreementRequest build() {
        return new CreateAgreementRequest(this);
    }

    public CreateAgreementRequestBuilder reference(String reference) {
        this.reference = reference;
        return this;
    }

    public CreateAgreementRequestBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CreateAgreementRequestBuilder userIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }
}
