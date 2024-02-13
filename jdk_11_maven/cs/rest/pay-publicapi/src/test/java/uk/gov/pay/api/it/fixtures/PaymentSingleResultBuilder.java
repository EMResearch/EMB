package uk.gov.pay.api.it.fixtures;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.List;
import java.util.Map;

public class PaymentSingleResultBuilder extends PaymentResultBuilder {
    
    public static PaymentSingleResultBuilder aSuccessfulSinglePayment() {
        return new PaymentSingleResultBuilder();
    }


    public PaymentSingleResultBuilder withCardDetails(CardDetailsFromResponse cardDetails) {
        this.cardDetails = new CardDetails(cardDetails);
        return this;
    }

    public PaymentSingleResultBuilder withMatchingReference(String reference) {
        this.reference = reference;
        return this;
    }

    public PaymentSingleResultBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public PaymentSingleResultBuilder withLanguage(SupportedLanguage language) {
        this.language = language.toString();
        return this;
    }

    public PaymentSingleResultBuilder withDelayedCapture(boolean delayedCapture) {
        this.delayedCapture = delayedCapture;
        return this;
    }

    public PaymentSingleResultBuilder withMoto(boolean moto) {
        this.moto = moto;
        return this;
    }

    public PaymentSingleResultBuilder withAgreementId(String agreementId) {
        this.agreementId = agreementId;
        return this;
    }

    public PaymentSingleResultBuilder withCorporateCardSurcharge(Long surcharge) {
        this.corporateCardSurcharge = surcharge;
        return this;
    }
    
    public PaymentSingleResultBuilder withTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public PaymentSingleResultBuilder withFee(Long fee) {
        this.fee = fee;
        return this;
    }

    public PaymentSingleResultBuilder withNetAmount(Long netAmount) {
        this.netAmount = netAmount;
        return this;
    }
    
    public PaymentSingleResultBuilder withAmount(long amount) {
        this.amount = amount;
        return this;
    }
    
    public PaymentSingleResultBuilder withDescription(String description) {
        this.description = description;
        return this;
    }
    
    public PaymentSingleResultBuilder withChargeId(String chargeId) {
        this.chargeId = chargeId;
        return this;
    }
    
    public PaymentSingleResultBuilder withState(PaymentState paymentState) {
        this.state = paymentState == null ?
                new TestPaymentState("submitted", false) :
                paymentState.getCanRetry() == null ?
                new TestPaymentState(paymentState.getStatus(), paymentState.isFinished()) :
                new TestPaymentRejectedState(paymentState.getStatus(), paymentState.getCode(), paymentState.getMessage(), paymentState.getCanRetry());
        return this;
    }
    
    public PaymentSingleResultBuilder withReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
        return this;
    }
    
    public PaymentSingleResultBuilder withPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }
    
    public PaymentSingleResultBuilder withCreatedDate(String createdDate) {
        this.createdDate = createdDate;
        return this;
    }
    
    public PaymentSingleResultBuilder withLinks(List<Map< ?,? >> links) {
        this.links = links;
        return this;
    }
    
    public PaymentSingleResultBuilder withRefundSummary(uk.gov.pay.api.model.RefundSummary refundSummary) {
        this.refundSummary = new RefundSummary(refundSummary);
        return this;
    }
    
    public PaymentSingleResultBuilder withSettlementSummary(PaymentSettlementSummary settlementSummary) {
        this.settlementSummary = settlementSummary == null ? new SettlementSummary() : new SettlementSummary(settlementSummary);
        return this;
    }
    
    public PaymentSingleResultBuilder withGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
        return this;
    }

    public PaymentSingleResultBuilder withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public PaymentSingleResultBuilder withAuthorisationSummary(uk.gov.pay.api.model.AuthorisationSummary authorisationSummary) {
        this.authorisationSummary = authorisationSummary == null ? null : new AuthorisationSummary(authorisationSummary.getThreeDSecure());
        return this;
    }
    
    public PaymentSingleResultBuilder withAuthorisationMode(AuthorisationMode authorisationMode) {
        this.authorisationMode = authorisationMode;
        return this;
    }

    public PaymentSingleResultBuilder withWalletType(String walletType) {
        this.walletType = walletType;
        return this;
    }

    public String build() {
        TestPayment result = getPayment();
        return new Gson().toJson(result, new TypeReference<TestPayment>() {}.getType());
    }
}
