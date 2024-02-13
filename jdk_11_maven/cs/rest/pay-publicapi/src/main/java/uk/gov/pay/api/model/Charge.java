package uk.gov.pay.api.model;

import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.util.List;
import java.util.Optional;

public class Charge {

    private String chargeId;

    private String returnUrl;

    private String paymentProvider;

    private List<PaymentConnectorResponseLink> links;

    private RefundSummary refundSummary;

    private PaymentSettlementSummary settlementSummary;

    private CardDetails cardDetails;

    private Long amount;

    private PaymentState state;

    private String description;

    private String reference;

    private String email;

    private SupportedLanguage language;

    private boolean delayedCapture;

    private boolean moto;

    private String agreementId;

    private Long corporateCardSurcharge;

    private Long totalAmount;

    private Long fee;

    private Long netAmount;

    private String createdDate;

    private String gatewayTransactionId;

    private ExternalMetadata metadata;

    private AuthorisationSummary authorisationSummary;

    private AuthorisationMode authorisationMode;

    public Charge(String chargeId, Long amount, PaymentState state, String returnUrl, String description,
                  String reference, String email, String paymentProvider, String createdDate,
                  SupportedLanguage language, boolean delayedCapture, boolean moto, RefundSummary refundSummary,
                  PaymentSettlementSummary settlementSummary, CardDetails cardDetails,
                  List<PaymentConnectorResponseLink> links, Long corporateCardSurcharge, Long totalAmount,
                  String gatewayTransactionId, ExternalMetadata metadata, Long fee, Long netAmount,
                  AuthorisationSummary authorisationSummary, String agreementId, AuthorisationMode authorisationMode) {
        this.chargeId = chargeId;
        this.amount = amount;
        this.state = state;
        this.returnUrl = returnUrl;
        this.description = description;
        this.reference = reference;
        this.email = email;
        this.paymentProvider = paymentProvider;
        this.createdDate = createdDate;
        this.language = language;
        this.delayedCapture = delayedCapture;
        this.moto = moto;
        this.refundSummary = refundSummary;
        this.settlementSummary = settlementSummary;
        this.cardDetails = cardDetails;
        this.links = links;
        this.corporateCardSurcharge = corporateCardSurcharge;
        this.totalAmount = totalAmount;
        this.gatewayTransactionId = gatewayTransactionId;
        this.metadata = metadata;
        this.fee = fee;
        this.netAmount = netAmount;
        this.authorisationSummary = authorisationSummary;
        this.agreementId = agreementId;
        this.authorisationMode = authorisationMode;
    }

    public static Charge from(ChargeFromResponse chargeFromResponse) {
        return new Charge(
                chargeFromResponse.getChargeId(),
                chargeFromResponse.getAmount(),
                chargeFromResponse.getState(),
                chargeFromResponse.getReturnUrl(),
                chargeFromResponse.getDescription(),
                chargeFromResponse.getReference(),
                chargeFromResponse.getEmail(),
                chargeFromResponse.getPaymentProvider(),
                chargeFromResponse.getCreatedDate(),
                chargeFromResponse.getLanguage(),
                chargeFromResponse.getDelayedCapture(),
                chargeFromResponse.isMoto(),
                chargeFromResponse.getRefundSummary(),
                chargeFromResponse.getSettlementSummary(),
                chargeFromResponse.getWalletType()
                        .map(wallet -> CardDetails.from(chargeFromResponse.getCardDetailsFromResponse(), wallet.getTitleCase()))
                        .orElse(CardDetails.from(chargeFromResponse.getCardDetailsFromResponse(), null)),
                chargeFromResponse.getLinks(),
                chargeFromResponse.getCorporateCardSurcharge(),
                chargeFromResponse.getTotalAmount(),
                chargeFromResponse.getGatewayTransactionId(),
                chargeFromResponse.getMetadata().orElse(null),
                chargeFromResponse.getFee(),
                chargeFromResponse.getNetAmount(),
                chargeFromResponse.getAuthorisationSummary(),
                chargeFromResponse.getAgreementId(),
                chargeFromResponse.getAuthorisationMode()
        );
    }

    public static Charge from(TransactionResponse transactionResponse) {
        return new Charge(
                transactionResponse.getTransactionId(),
                transactionResponse.getAmount(),
                transactionResponse.getState(),
                transactionResponse.getReturnUrl(),
                transactionResponse.getDescription(),
                transactionResponse.getReference(),
                transactionResponse.getEmail(),
                transactionResponse.getPaymentProvider(),
                transactionResponse.getCreatedDate(),
                transactionResponse.getLanguage(),
                transactionResponse.getDelayedCapture(),
                transactionResponse.isMoto(),
                transactionResponse.getRefundSummary(),
                transactionResponse.getSettlementSummary(),
                transactionResponse.getWalletType()
                        .map(wallet -> CardDetails.from(transactionResponse.getCardDetailsFromResponse(), wallet.getTitleCase()))
                        .orElse(CardDetails.from(transactionResponse.getCardDetailsFromResponse(), null)),
                transactionResponse.getLinks(),
                transactionResponse.getCorporateCardSurcharge(),
                transactionResponse.getTotalAmount(),
                transactionResponse.getGatewayTransactionId(),
                transactionResponse.getMetadata().orElse(null),
                transactionResponse.getFee(),
                transactionResponse.getNetAmount(),
                transactionResponse.getAuthorisationSummary(),
                null,
                transactionResponse.getAuthorisationMode());
    }

    public Optional<ExternalMetadata> getMetadata() {
        return Optional.ofNullable(metadata);
    }

    public String getChargeId() {
        return chargeId;
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentState getState() {
        return state;
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

    public SupportedLanguage getLanguage() {
        return language;
    }

    public boolean getDelayedCapture() {
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

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public String getCardBrand() {
        return cardDetails != null ? cardDetails.getCardBrand() : "";
    }

    public String getCreatedDate() {
        return createdDate;
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

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public AuthorisationMode getAuthorisationMode() {
        return authorisationMode;
    }
}
