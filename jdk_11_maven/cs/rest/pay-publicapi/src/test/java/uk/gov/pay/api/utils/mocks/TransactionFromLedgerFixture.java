package uk.gov.pay.api.utils.mocks;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.AuthorisationSummary;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentConnectorResponseLink;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionFromLedgerFixture {
    private String transactionId;
    private String returnUrl;
    private String paymentProvider;
    private List<PaymentConnectorResponseLink> links;
    private RefundSummary refundSummary;
    private PaymentSettlementSummary settlementSummary;
    private CardDetailsFromResponse cardDetails;
    private Long amount;
    private PaymentState state;
    private String description;
    private String reference;
    private String email;
    private String language;
    private boolean delayedCapture;
    private boolean moto;
    private Long corporateCardSurcharge;
    private Long totalAmount;
    private Long fee;
    private Long netAmount;
    private String createdDate;
    private String gatewayTransactionId;
    private Map<String, Object> metadata;
    private AuthorisationSummary authorisationSummary;
    private AuthorisationMode authorisationMode;
    private final Optional<String> walletType;

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public List<PaymentConnectorResponseLink> getLinks() {
        return links;
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

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public String getEmail() {
        return email;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDelayedCapture() {
        return delayedCapture;
    }

    public boolean isMoto() {
        return moto;
    }

    public Long getCorporateCardSurcharge() {
        return corporateCardSurcharge;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public Long getFee() {
        return fee;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    }

    public AuthorisationMode getAuthorisationMode() {
        return authorisationMode;
    }

    public Optional<String> getWalletType() { return walletType; }

    public TransactionFromLedgerFixture(TransactionFromLedgerBuilder builder) {
        this.amount = builder.amount;
        this.state = builder.state;
        this.createdDate = builder.createdDate;
        this.transactionId = builder.transactionId;
        this.returnUrl = builder.returnUrl;
        this.paymentProvider = builder.paymentProvider;
        this.links = builder.links;
        this.refundSummary = builder.refundSummary;
        this.settlementSummary = builder.settlementSummary;
        this.cardDetails = builder.cardDetails;
        this.description = builder.description;
        this.reference = builder.reference;
        this.email = builder.email;
        this.language = builder.language;
        this.delayedCapture = builder.delayedCapture;
        this.moto = builder.moto;
        this.corporateCardSurcharge = builder.corporateCardSurcharge;
        this.totalAmount = builder.totalAmount;
        this.fee = builder.fee;
        this.netAmount = builder.netAmount;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.metadata = builder.metadata;
        this.authorisationSummary = builder.authorisationSummary;
        this.authorisationMode = builder.authorisationMode;
        this.walletType = builder.walletType == null || builder.walletType.isEmpty() ? Optional.empty() : Optional.of(builder.walletType);
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentState getState() {
        return state;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getTransactionId() {
        return transactionId;
    }


    public static final class TransactionFromLedgerBuilder {
        private String transactionId;
        private String returnUrl;
        private String paymentProvider;
        private List<PaymentConnectorResponseLink> links = new ArrayList<>();
        private RefundSummary refundSummary;
        private PaymentSettlementSummary settlementSummary;
        private CardDetailsFromResponse cardDetails;
        private Long amount;
        private PaymentState state;
        private String description;
        private String reference;
        private String email;
        private String language;
        private boolean delayedCapture;
        private boolean moto;
        private Long corporateCardSurcharge;
        private Long totalAmount;
        private Long fee;
        private Long netAmount;
        private String createdDate;
        private String gatewayTransactionId;
        private Map<String, Object> metadata;
        private AuthorisationSummary authorisationSummary;
        private AuthorisationMode authorisationMode = AuthorisationMode.WEB;
        private String walletType;


        private TransactionFromLedgerBuilder() {
        }

        public static TransactionFromLedgerBuilder aTransactionFromLedgerFixture() {
            return new TransactionFromLedgerBuilder();
        }

        public TransactionFromLedgerBuilder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public TransactionFromLedgerBuilder withState(PaymentState state) {
            this.state = state;
            return this;
        }

        public TransactionFromLedgerBuilder withCreatedDate(String createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public TransactionFromLedgerBuilder withTransactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public TransactionFromLedgerBuilder withReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public TransactionFromLedgerBuilder withPaymentProvider(String paymentProvider) {
            this.paymentProvider = paymentProvider;
            return this;
        }

        public TransactionFromLedgerBuilder withLinks(List<PaymentConnectorResponseLink> links) {
            this.links = links;
            return this;
        }

        public TransactionFromLedgerBuilder withRefundSummary(RefundSummary refundSummary) {
            this.refundSummary = refundSummary;
            return this;
        }

        public TransactionFromLedgerBuilder withSettlementSummary(PaymentSettlementSummary settlementSummary) {
            this.settlementSummary = settlementSummary;
            return this;
        }

        public TransactionFromLedgerBuilder withCardDetails(CardDetailsFromResponse cardDetails) {
            this.cardDetails = cardDetails;
            return this;
        }

        public TransactionFromLedgerBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public TransactionFromLedgerBuilder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public TransactionFromLedgerBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public TransactionFromLedgerBuilder withLanguage(SupportedLanguage language) {
            this.language = language.toString();
            return this;
        }

        public TransactionFromLedgerBuilder withDelayedCapture(boolean delayedCapture) {
            this.delayedCapture = delayedCapture;
            return this;
        }
        
        public TransactionFromLedgerBuilder withMoto(boolean moto) {
            this.moto = moto;
            return this;
        }

        public TransactionFromLedgerBuilder withCorporateCardSurcharge(Long corporateCardSurcharge) {
            this.corporateCardSurcharge = corporateCardSurcharge;
            return this;
        }

        public TransactionFromLedgerBuilder withTotalAmount(Long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public TransactionFromLedgerBuilder withFee(Long fee) {
            this.fee = fee;
            return this;
        }

        public TransactionFromLedgerBuilder withNetAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public TransactionFromLedgerBuilder withGatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public TransactionFromLedgerBuilder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public TransactionFromLedgerBuilder withAuthorisationSummary(AuthorisationSummary authorisationSummary) {
            this.authorisationSummary = authorisationSummary;
            return this;
        }
        
        public TransactionFromLedgerBuilder withAuthorisationMode(AuthorisationMode authorisationMode) {
            this.authorisationMode = authorisationMode;
            return this;
        }

        public TransactionFromLedgerBuilder withWalletType(String walletType) {
            this.walletType = walletType;
            return this;
        }

        public TransactionFromLedgerFixture build() {
            return new TransactionFromLedgerFixture(this);
        }
    }
}
