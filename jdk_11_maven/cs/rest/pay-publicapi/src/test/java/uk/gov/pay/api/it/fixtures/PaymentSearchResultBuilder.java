package uk.gov.pay.api.it.fixtures;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.service.payments.commons.model.AuthorisationMode;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class PaymentSearchResultBuilder extends PaymentResultBuilder {

    private int noOfResults = DEFAULT_NUMBER_OF_RESULTS;


    public static PaymentSearchResultBuilder aSuccessfulSearchPayment() {
        return new PaymentSearchResultBuilder();
    }

    public PaymentSearchResultBuilder withCaptureLink(String href) {
        this.links.add(ImmutableMap.of(
                "href", href,
                "rel", "capture",
                "method", "POST"));
        return this;
    }

    public PaymentSearchResultBuilder withChargeId(String chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public PaymentSearchResultBuilder withCardDetails(CardDetailsFromResponse cardDetails) {
        this.cardDetails = new CardDetails(cardDetails);
        return this;
    }

    public PaymentSearchResultBuilder withReference(String reference) {
        this.reference = reference;
        return this;
    }
    
    public PaymentSearchResultBuilder withReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
        return this;
    }

    public PaymentSearchResultBuilder withFee(Long fee) {
        this.fee = fee;
        return this;
    }

    public PaymentSearchResultBuilder withNetAmount(Long netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public PaymentSearchResultBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public PaymentSearchResultBuilder withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public PaymentSearchResultBuilder withGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
        return this;
    }

    public PaymentSearchResultBuilder withInProgressState(String status) {
        this.state = new TestPaymentState(status, false);
        return this;
    }

    public PaymentSearchResultBuilder withSuccessState(String status) {
        this.state = new TestPaymentSuccessState(status);
        return this;
    }

    public PaymentSearchResultBuilder withRejectedState(String status, String code, String message,  boolean canRetry) {
        this.state = new TestPaymentRejectedState(status, code, message, canRetry);
        return this;
    }

    public PaymentSearchResultBuilder withDelayedCapture(boolean delayedCapture) {
        this.delayedCapture = delayedCapture;
        return this;
    }

    public PaymentSearchResultBuilder withCreatedDateBetween(String fromDate, String toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        return this;
    }

    public PaymentSearchResultBuilder withNumberOfResults(int numberOfResults) {
        this.noOfResults = numberOfResults;
        return this;
    }

    public PaymentSearchResultBuilder withSettlementSummary(PaymentSettlementSummary settlementSummary) {
        this.settlementSummary = new SettlementSummary(settlementSummary);
        return this;
    }

    public PaymentSearchResultBuilder withAuthorisationSummary(uk.gov.pay.api.model.AuthorisationSummary authorisationSummary) {
        this.authorisationSummary = authorisationSummary == null ? null : new AuthorisationSummary(authorisationSummary.getThreeDSecure());
        return this;
    }
    
    public PaymentSearchResultBuilder withAuthorisationMode(AuthorisationMode authorisationMode) {
        this.authorisationMode = authorisationMode;
        return this;
    }

    public PaymentSearchResultBuilder withWalletType(String walletType) {
        this.walletType = walletType;
        return this;
    }

    public List<TestPayment> getResults() {
        List<TestPayment> results = newArrayList();
        for (int i = 0; i < noOfResults; i++) {
            results.add(getPayment(i));
        }
        return results;
    }

    public String build() {
        List<TestPayment> results = getResults();

        return new GsonBuilder().create().toJson(
                ImmutableMap.of("results", results),
                new TypeToken<Map<String, List<TestPayment>>>() {
                }.getType()
        );
    }
}
