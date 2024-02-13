package uk.gov.pay.api.utils.mocks;
import java.util.List;
import java.util.Objects;

public class AgreementResponseFromConnector {
    private final String agreementId;
    private final String reference;
    private final String createdDate;
    private final String serviceId;
    private final boolean live;

    public String getAgreementId() {
        return agreementId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean isLive() {
        return live;
    }

    public String getReference() {
        return reference;
    }
    
    private AgreementResponseFromConnector(AgreementResponseFromConnectorBuilder builder) {
        this.createdDate = builder.createdDate;
        this.agreementId = builder.agreementId;
        this.reference = builder.reference;
        this.serviceId = builder.serviceId;
        this.live = builder.live;
    }

    public static final class AgreementResponseFromConnectorBuilder {
        private String agreementId;
        private String reference;
        private String createdDate;
        private String serviceId;
        private boolean live;
        
        private AgreementResponseFromConnectorBuilder() {
        }

        public static AgreementResponseFromConnectorBuilder aCreateAgreementResponseFromConnector() {
            return new AgreementResponseFromConnectorBuilder();
        }

        public static AgreementResponseFromConnectorBuilder aCreateAgreementResponseFromConnector(AgreementResponseFromConnector responseFromConnector) {
            return new AgreementResponseFromConnectorBuilder()
                    .withReference(responseFromConnector.reference)
                    .withCreatedDate(responseFromConnector.createdDate)
                    .withAgreementId(responseFromConnector.agreementId)
                    .withServiceId(responseFromConnector.serviceId)
                    .withLive(responseFromConnector.live);
        }

        public AgreementResponseFromConnectorBuilder withAgreementId(String agreementId) {
            this.agreementId = agreementId;
            return this;
        }

        public AgreementResponseFromConnectorBuilder withServiceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public AgreementResponseFromConnectorBuilder withLive(boolean isLive) {
            this.live = isLive;
            return this;
        }
        
        public AgreementResponseFromConnectorBuilder withReference(String reference) {
            this.reference = reference;
            return this;
        }
       
        public AgreementResponseFromConnectorBuilder withCreatedDate(String createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public AgreementResponseFromConnector build() {
            List.of(agreementId, serviceId,createdDate, reference).forEach(Objects::requireNonNull);
            return new AgreementResponseFromConnector(this);
        }
    }
}
