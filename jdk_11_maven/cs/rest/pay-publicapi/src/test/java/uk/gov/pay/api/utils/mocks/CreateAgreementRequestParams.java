package uk.gov.pay.api.utils.mocks;

import java.util.List;
import java.util.Objects;

public class CreateAgreementRequestParams {
    private final String reference;
    private final String description;
    private final String userIdentifier;

    private CreateAgreementRequestParams(CreateAgreementRequestParamsBuilder builder) {
        this.reference = builder.reference;
        this.description = builder.description;
        this.userIdentifier = builder.userIdentifier;
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

    public static final class CreateAgreementRequestParamsBuilder {
        private String reference;
        private String description;
        private String userIdentifier;

        private CreateAgreementRequestParamsBuilder() {
        }

        public static CreateAgreementRequestParamsBuilder aCreateAgreementRequestParams() {
            return new CreateAgreementRequestParamsBuilder();
        }

        public CreateAgreementRequestParamsBuilder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public CreateAgreementRequestParamsBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public CreateAgreementRequestParamsBuilder withUserIdentifier(String userIdentifier) {
            this.userIdentifier = userIdentifier;
            return this;
        }

        public CreateAgreementRequestParams build() {
            List.of(reference).forEach(Objects::requireNonNull);
            return new CreateAgreementRequestParams(this);
        }
    }
}
