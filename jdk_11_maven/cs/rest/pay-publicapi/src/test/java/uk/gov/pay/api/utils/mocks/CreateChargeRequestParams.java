package uk.gov.pay.api.utils.mocks;

import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.Source;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CreateChargeRequestParams {
    
    private final int amount;
    private final String returnUrl, description, reference;
    private final Map<String, Object> metadata;
    private final Boolean moto;
    private final String email;
    private final String cardholderName;
    private final String addressLine1;
    private final String addressLine2;
    private final String addressPostcode;
    private final String addressCity;
    private final String addressCountry;
    private final SupportedLanguage language;
    private final Source source;
    private final String setUpAgreement;
    private final AuthorisationMode authorisationMode;
    private final String agreementId;

    private CreateChargeRequestParams(CreateChargeRequestParamsBuilder builder) {
        this.amount = builder.amount;
        this.returnUrl = builder.returnUrl;
        this.description = builder.description;
        this.reference = builder.reference;
        this.metadata = builder.metadata;
        this.moto = builder.moto;
        this.email = builder.email;
        this.cardholderName = builder.cardholderName;
        this.addressLine1 = builder.addressLine1;
        this.addressLine2 = builder.addressLine2;
        this.addressPostcode = builder.addressPostcode;
        this.addressCity = builder.addressCity;
        this.addressCountry = builder.addressCountry;
        this.language = builder.language;
        this.source = builder.source;
        this.setUpAgreement = builder.setUpAgreement;
        this.authorisationMode = builder.authorisationMode;
        this.agreementId = builder.agreementId;
    }

    public int getAmount() {
        return amount;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Boolean isMoto() {
        return moto;
    }

    public String getEmail() {
        return email;
    }

    public Optional<String> getCardholderName() {
        return Optional.ofNullable(cardholderName);
    }

    public Optional<String> getAddressLine1() {
        return Optional.ofNullable(addressLine1);
    }

    public Optional<String> getAddressLine2() {
        return Optional.ofNullable(addressLine2);
    }

    public Optional<String> getAddressPostcode() {
        return Optional.ofNullable(addressPostcode);
    }

    public Optional<String> getAddressCity() {
        return Optional.ofNullable(addressCity);
    }

    public Optional<String> getAddressCountry() {
        return Optional.ofNullable(addressCountry);
    }

    public SupportedLanguage getLanguage() {
        return language;
    }

    public Optional<Source> getSource() {
        return Optional.ofNullable(source);
    }

    public Optional<String> getSetUpAgreement() {
        return Optional.ofNullable(setUpAgreement);
    }

    public Optional<AuthorisationMode> getAuthorisationMode() {
        return Optional.ofNullable(authorisationMode);
    }

    public Optional<String> getAgreementId() {
        return Optional.ofNullable(agreementId);
    }

    public static final class CreateChargeRequestParamsBuilder {
        private Integer amount;
        private String returnUrl;
        private String description;
        private String reference;
        private Map<String, Object> metadata = Map.of();
        private Boolean moto;
        private String email;
        private String cardholderName;
        private String addressLine1;
        private String addressLine2;
        private String addressPostcode;
        private String addressCity;
        private String addressCountry;
        private SupportedLanguage language;
        private Source source;
        public String setUpAgreement;
        public AuthorisationMode authorisationMode;
        public String agreementId;

        private CreateChargeRequestParamsBuilder() {
        }

        public static CreateChargeRequestParamsBuilder aCreateChargeRequestParams() {
            return new CreateChargeRequestParamsBuilder();
        }

        public CreateChargeRequestParamsBuilder withAmount(int amount) {
            this.amount = amount;
            return this;
        }

        public CreateChargeRequestParamsBuilder withReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public CreateChargeRequestParamsBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public CreateChargeRequestParamsBuilder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public CreateChargeRequestParamsBuilder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public CreateChargeRequestParamsBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public CreateChargeRequestParamsBuilder witCardHolderName(String cardholderName) {
            this.cardholderName = cardholderName;
            return this;
        }
        
        public CreateChargeRequestParamsBuilder withAddressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        public CreateChargeRequestParamsBuilder withAddressLine2(String addressLine2) {
            this.addressLine2 = addressLine2;
            return this;
        }

        public CreateChargeRequestParamsBuilder withAddressPostcode(String addressPostcode) {
            this.addressPostcode = addressPostcode;
            return this;
        }

        public CreateChargeRequestParamsBuilder withAddressCity(String addressCity) {
            this.addressCity = addressCity;
            return this;
        }

        public CreateChargeRequestParamsBuilder withAddressCountry(String addressCountry) {
            this.addressCountry = addressCountry;
            return this;
        }

        public CreateChargeRequestParams build() {
            List.of(amount, reference, description).forEach(Objects::requireNonNull);
            return new CreateChargeRequestParams(this);
        }

        public CreateChargeRequestParamsBuilder withLanguage(SupportedLanguage language) {
            this.language = language;
            return this;
        }

        public CreateChargeRequestParamsBuilder withSource(Source source) {
            this.source = source;
            return this;
        }

        public CreateChargeRequestParamsBuilder withMoto(boolean moto) {
            this.moto = moto;
            return this;
        }

        public CreateChargeRequestParamsBuilder withSetUpAgreement(String setUpAgreement) {
            this.setUpAgreement = setUpAgreement;
            return this;
        }
        
        public CreateChargeRequestParamsBuilder withAuthorisationMode(AuthorisationMode authorisationMode) {
            this.authorisationMode = authorisationMode;
            return this;
        }

        public CreateChargeRequestParamsBuilder withAgreementId(String agreementId) {
            this.agreementId = agreementId;
            return this;
        }
    }
}
