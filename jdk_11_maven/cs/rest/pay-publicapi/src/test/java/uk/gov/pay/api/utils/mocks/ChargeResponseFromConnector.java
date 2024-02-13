package uk.gov.pay.api.utils.mocks;

import uk.gov.pay.api.model.AuthorisationSummary;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.model.telephone.PaymentOutcome;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ChargeResponseFromConnector {
    private final Long amount, corporateCardSurcharge, totalAmount; 
    private final PaymentState state; 
    private final String chargeId, returnUrl, description, reference, email, telephoneNumber, paymentProvider, gatewayTransactionId, processorId, providerId, authCode, createdDate, authorisedDate;
    private final SupportedLanguage language;
    private final PaymentOutcome paymentOutcome;
    private final boolean delayedCapture; 
    private final boolean moto;
    private final RefundSummary refundSummary; 
    private final PaymentSettlementSummary settlementSummary;
    private final CardDetailsFromResponse cardDetails;
    private final List<Map<?, ?>> links;
    private final Optional<Map<String, Object>> metadata;
    private final Long fee;
    private final Long netAmount;
    private final AuthorisationSummary authorisationSummary;
    private final String agreementId;
    private final AuthorisationMode authorisationMode;
    private final String walletType;

    public Long getAmount() {
        return amount;
    }

    public Long getCorporateCardSurcharge() {
        return corporateCardSurcharge;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public PaymentState getState() {
        return state;
    }

    public String getChargeId() {
        return chargeId;
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

    public String getEmail() {
        return email;
    }
    
    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }
    
    public String getProcessorId() {
        return processorId;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public String getAuthCode() {
        return authCode;
    }

    public String getCreatedDate() {
        return createdDate;
    }
    
    public String getAuthorisedDate() {
        return authorisedDate;
    }

    public SupportedLanguage getLanguage() {
        return language;
    }

    public PaymentOutcome getPaymentOutcome() {
        return paymentOutcome;
    }

    public boolean isDelayedCapture() {
        return delayedCapture;
    }

    public boolean isMoto() {
        return moto;
    }

    public RefundSummary getRefundSummary() {
        return refundSummary;
    }

    public PaymentSettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public CardDetailsFromResponse getCardDetails() {
        return cardDetails;
    }

    public List<Map<?, ?>> getLinks() {
        return links;
    }

    public Optional<Map<String, Object>> getMetadata() {
        return metadata;
    }

    public Long getFee() {
        return fee;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    }

    public String getAgreementId() {
        return this.agreementId;
    }
    
    public AuthorisationMode getAuthorisationMode() {
        return authorisationMode;
    }
    
    public String getWalletType() { return walletType; }

    private ChargeResponseFromConnector(ChargeResponseFromConnectorBuilder builder) {
        this.amount = builder.amount;
        this.chargeId = builder.chargeId;
        this.state = builder.state;
        this.returnUrl = builder.returnUrl;
        this.description = builder.description;
        this.reference = builder.reference;
        this.email = builder.email;
        this.telephoneNumber = builder.telephoneNumber;
        this.paymentProvider = builder.paymentProvider;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.processorId = builder.processorId;
        this.providerId = builder.providerId;
        this.authCode = builder.authCode;
        this.createdDate = builder.createdDate;
        this.authorisedDate = builder.authorisedDate;
        this.language = builder.language;
        this.paymentOutcome = builder.paymentOutcome;
        this.delayedCapture = builder.delayedCapture;
        this.moto = builder.moto;
        this.corporateCardSurcharge = builder.corporateCardSurcharge;
        this.totalAmount = builder.totalAmount;
        this.refundSummary = builder.refundSummary;
        this.settlementSummary = builder.settlementSummary;
        this.cardDetails = builder.cardDetails;
        this.links = builder.links;
        this.metadata = builder.metadata == null || builder.metadata.isEmpty() ? Optional.empty() : Optional.of(builder.metadata);
        this.fee = builder.fee;
        this.netAmount = builder.netAmount;
        this.authorisationSummary = builder.authorisationSummary;
        this.agreementId = builder.agreementId;
        this.authorisationMode = builder.authorisationMode;
        this.walletType = builder.walletType;
    }

    public static final class ChargeResponseFromConnectorBuilder {
        private Long amount, corporateCardSurcharge, totalAmount;
        private PaymentState state;
        private String chargeId, returnUrl, description, reference, email, telephoneNumber, paymentProvider, gatewayTransactionId, processorId, providerId, authCode, createdDate, authorisedDate;
        private PaymentOutcome paymentOutcome;
        private SupportedLanguage language;
        private Boolean delayedCapture;
        private boolean moto;
        private RefundSummary refundSummary;
        private PaymentSettlementSummary settlementSummary;
        private CardDetailsFromResponse cardDetails;
        private List<Map<?, ?>> links = new ArrayList<>();
        private Map<String, Object> metadata = Map.of();
        private Long fee = null;
        private Long netAmount = null;
        private AuthorisationSummary authorisationSummary = null;
        private String agreementId;
        private AuthorisationMode authorisationMode = AuthorisationMode.WEB;
        private String walletType = null;
        
        private ChargeResponseFromConnectorBuilder() {
        }

        public static ChargeResponseFromConnectorBuilder aCreateOrGetChargeResponseFromConnector() {
            return new ChargeResponseFromConnectorBuilder();
        }

        public static ChargeResponseFromConnectorBuilder aCreateOrGetChargeResponseFromConnector(ChargeResponseFromConnector responseFromConnector) {
            return new ChargeResponseFromConnectorBuilder()
                    .withAmount(responseFromConnector.amount)
                    .withChargeId(responseFromConnector.chargeId)
                    .withState(responseFromConnector.state)
                    .withReturnUrl(responseFromConnector.returnUrl)
                    .withDescription(responseFromConnector.description)
                    .withReference(responseFromConnector.reference)
                    .withEmail(responseFromConnector.email)
                    .withPaymentProvider(responseFromConnector.paymentProvider)
                    .withGatewayTransactionId(responseFromConnector.gatewayTransactionId)
                    .withCreatedDate(responseFromConnector.createdDate)
                    .withLanguage(responseFromConnector.language)
                    .withDelayedCapture(responseFromConnector.delayedCapture)
                    .withMoto(responseFromConnector.moto)
                    .withCorporateCardSurcharge(responseFromConnector.corporateCardSurcharge)
                    .withTotalAmount(responseFromConnector.totalAmount)
                    .withRefundSummary(responseFromConnector.refundSummary)
                    .withSettlementSummary(responseFromConnector.settlementSummary)
                    .withCardDetails(responseFromConnector.cardDetails)
                    .withMetadata(responseFromConnector.metadata.orElse(null))
                    .withNetAmount(responseFromConnector.getNetAmount())
                    .withFee(responseFromConnector.getFee())
                    .withAuthorisationSummary(responseFromConnector.getAuthorisationSummary())
                    .withAgreementId(responseFromConnector.getAgreementId())
                    .withAuthorisationMode(responseFromConnector.getAuthorisationMode())
                    .withWalletType(responseFromConnector.walletType);
        }

        public ChargeResponseFromConnectorBuilder withAmount(long amount) {
            this.amount = amount;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withChargeId(String chargeId) {
            this.chargeId = chargeId;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withState(PaymentState state) {
            this.state = state;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withTelephoneNumber(String telephoneNumber) {
            this.telephoneNumber = telephoneNumber;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withPaymentProvider(String paymentProvider) {
            this.paymentProvider = paymentProvider;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withProcessorId(String processorId) {
            this.processorId = processorId;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withProviderId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withGatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withAuthCode(String authCode) {
            this.authCode = authCode;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withCreatedDate(String createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withAuthorisedDate(String authorisedDate) {
            this.authorisedDate = authorisedDate;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withLanguage(SupportedLanguage language) {
            this.language = language;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withPaymentOutcome(PaymentOutcome paymentOutcome) {
            this.paymentOutcome = paymentOutcome;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withDelayedCapture(boolean delayedCapture) {
            this.delayedCapture = delayedCapture;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withMoto(boolean moto) {
            this.moto = moto;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withCorporateCardSurcharge(Long corporateCardSurcharge) {
            this.corporateCardSurcharge = corporateCardSurcharge;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withTotalAmount(Long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withRefundSummary(RefundSummary refundSummary) {
            this.refundSummary = refundSummary;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withSettlementSummary(PaymentSettlementSummary settlementSummary) {
            this.settlementSummary = settlementSummary;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withCardDetails(CardDetailsFromResponse cardDetails) {
            this.cardDetails = cardDetails;
            return this;
        }
        
        public ChargeResponseFromConnector buildTelephoneChargeResponse() {
            return new ChargeResponseFromConnector(this);
        }

        public ChargeResponseFromConnector build() {
            List.of(amount, chargeId, language, links).forEach(Objects::requireNonNull);
            
            return new ChargeResponseFromConnector(this);
        }

        public ChargeResponseFromConnectorBuilder withLink(Map<String, ?> link) {
            links.add(link);
            return this;
        }

        public ChargeResponseFromConnectorBuilder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public ChargeResponseFromConnectorBuilder withFee(Long fee) {
            this.fee = fee;
            return this;
        }
        
        public ChargeResponseFromConnectorBuilder withNetAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withAuthorisationSummary(AuthorisationSummary authorisationSummary) {
            this.authorisationSummary = authorisationSummary;
            return this;
        }

        public ChargeResponseFromConnectorBuilder withAgreementId(String agreementId) {
            this.agreementId = agreementId;
            return this;
        }
        
        public ChargeResponseFromConnectorBuilder withAuthorisationMode(AuthorisationMode authorisationMode) {
            this.authorisationMode = authorisationMode;
            return this;
        }
        
        public ChargeResponseFromConnectorBuilder withWalletType(String walletType) {
            this.walletType = walletType;
            return this;
        }
    }
    
}
