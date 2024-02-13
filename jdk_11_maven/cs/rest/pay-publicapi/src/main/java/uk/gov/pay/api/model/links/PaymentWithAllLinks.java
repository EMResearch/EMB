package uk.gov.pay.api.model.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.api.model.AuthorisationSummary;
import uk.gov.pay.api.model.CardDetails;
import uk.gov.pay.api.model.CardPayment;
import uk.gov.pay.api.model.Charge;
import uk.gov.pay.api.model.PaymentConnectorResponseLink;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.net.URI;
import java.util.List;

public class PaymentWithAllLinks extends CardPayment {

    @JsonProperty(CardPayment.LINKS_JSON_ATTRIBUTE)
    private PaymentLinks links = new PaymentLinks();

    public PaymentLinks getLinks() {
        return links;
    }

    private PaymentWithAllLinks(String chargeId, long amount, PaymentState state, String returnUrl, String description,
                               String reference, String email, String paymentProvider, String createdDate, SupportedLanguage language,
                               boolean delayedCapture, boolean moto, RefundSummary refundSummary, PaymentSettlementSummary settlementSummary, CardDetails cardDetails,
                               List<PaymentConnectorResponseLink> paymentConnectorResponseLinks, URI selfLink, URI paymentEventsUri, URI paymentCancelUri,
                               URI paymentRefundsUri, URI paymentCaptureUri, URI paymentAuthorisationUri,  Long corporateCardSurcharge, Long totalAmount, String providerId, ExternalMetadata metadata,
                               Long fee, Long netAmount, AuthorisationSummary authorisationSummary, String agreementId, AuthorisationMode authorisationMode) {
        super(chargeId, amount, state, returnUrl, description, reference, email, paymentProvider, createdDate,
                refundSummary, settlementSummary, cardDetails, language, delayedCapture, moto, corporateCardSurcharge, totalAmount,
                providerId, metadata, fee, netAmount, authorisationSummary, agreementId, authorisationMode);
        this.links.addSelf(selfLink.toString());
        this.links.addKnownLinksValueOf(paymentConnectorResponseLinks, paymentAuthorisationUri);
        this.links.addEvents(paymentEventsUri.toString());
        this.links.addRefunds(paymentRefundsUri.toString());

        if (!state.isFinished() && authorisationMode != AuthorisationMode.AGREEMENT) {
            this.links.addCancel(paymentCancelUri.toString());
        }

        if (paymentConnectorResponseLinks.stream().anyMatch(link -> "capture".equals(link.getRel()))) {
            this.links.addCapture(paymentCaptureUri.toString());
        }
    }

    public static PaymentWithAllLinks valueOf(Charge paymentConnector,
                                              URI selfLink,
                                              URI paymentEventsUri,
                                              URI paymentCancelUri,
                                              URI paymentRefundsUri,
                                              URI paymentsCaptureUri,
                                              URI paymentAuthorisationUri) {
        return new PaymentWithAllLinksBuilder()
                .withChargeId(paymentConnector.getChargeId())
                .withAmount(paymentConnector.getAmount())
                .withState(paymentConnector.getState())
                .withReturnUrl(paymentConnector.getReturnUrl())
                .withDescription(paymentConnector.getDescription())
                .withReference(paymentConnector.getReference())
                .withEmail(paymentConnector.getEmail())
                .withPaymentProvider(paymentConnector.getPaymentProvider())
                .withCreatedDate(paymentConnector.getCreatedDate())
                .withLanguage(paymentConnector.getLanguage())
                .withDelayedCapture(paymentConnector.getDelayedCapture())
                .withMoto(paymentConnector.isMoto())
                .withRefundSummary(paymentConnector.getRefundSummary())
                .withSettlementSummary(paymentConnector.getSettlementSummary())
                .withCardDetails(paymentConnector.getCardDetails())
                .withPaymentConnectorResponseLinks(paymentConnector.getLinks())
                .withSelfLink(selfLink)
                .withPaymentEventsUri(paymentEventsUri)
                .withPaymentCancelUri(paymentCancelUri)
                .withPaymentRefundsUri(paymentRefundsUri)
                .withPaymentCaptureUri(paymentsCaptureUri)
                .withPaymentAuthorisationUri(paymentAuthorisationUri)
                .withCorporateCardSurcharge(paymentConnector.getCorporateCardSurcharge())
                .withTotalAmount(paymentConnector.getTotalAmount())
                .withProviderId(paymentConnector.getGatewayTransactionId())
                .withMetadata(paymentConnector.getMetadata().orElse(null))
                .withFee(paymentConnector.getFee())
                .withNetAmount(paymentConnector.getNetAmount())
                .withAuthorisationSummary(paymentConnector.getAuthorisationSummary())
                .withAgreementId(paymentConnector.getAgreementId())
                .withAuthorisationMode(paymentConnector.getAuthorisationMode())
                .build();
    }

    public static PaymentWithAllLinks getPaymentWithLinks(
            Charge paymentConnector,
            URI selfLink,
            URI paymentEventsUri,
            URI paymentCancelUri,
            URI paymentRefundsUri,
            URI paymentsCaptureUri,
            URI paymentAuthorisationUri) {
        
        return PaymentWithAllLinks.valueOf(paymentConnector, selfLink, paymentEventsUri, paymentCancelUri, paymentRefundsUri, paymentsCaptureUri, paymentAuthorisationUri);
    }
    
    public static class PaymentWithAllLinksBuilder {
        private String chargeId;
        private long amount;
        private PaymentState state;
        private String returnUrl;
        private String description;
        private String reference;
        private String email;
        private String paymentProvider;
        private String createdDate;
        private SupportedLanguage language;
        private boolean delayedCapture;
        private boolean moto;
        private RefundSummary refundSummary;
        private PaymentSettlementSummary settlementSummary;
        private CardDetails cardDetails;
        private List<PaymentConnectorResponseLink> paymentConnectorResponseLinks;
        private URI selfLink;
        private URI paymentEventsUri;
        private URI paymentCancelUri;
        private URI paymentRefundsUri;
        private URI paymentCaptureUri;
        private URI paymentAuthorisationUri;
        private Long corporateCardSurcharge;
        private Long totalAmount;
        private String providerId;
        private ExternalMetadata metadata;
        private Long fee;
        private Long netAmount;
        private AuthorisationSummary authorisationSummary;
        private String agreementId;
        private AuthorisationMode authorisationMode;

        public PaymentWithAllLinksBuilder withChargeId(String chargeId) {
            this.chargeId = chargeId;
            return this;
        }

        public PaymentWithAllLinksBuilder withAmount(long amount) {
            this.amount = amount;
            return this;
        }

        public PaymentWithAllLinksBuilder withState(PaymentState state) {
            this.state = state;
            return this;
        }

        public PaymentWithAllLinksBuilder withReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        public PaymentWithAllLinksBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public PaymentWithAllLinksBuilder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public PaymentWithAllLinksBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public PaymentWithAllLinksBuilder withPaymentProvider(String paymentProvider) {
            this.paymentProvider = paymentProvider;
            return this;
        }

        public PaymentWithAllLinksBuilder withCreatedDate(String createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public PaymentWithAllLinksBuilder withLanguage(SupportedLanguage language) {
            this.language = language;
            return this;
        }

        public PaymentWithAllLinksBuilder withDelayedCapture(boolean delayedCapture) {
            this.delayedCapture = delayedCapture;
            return this;
        }

        public PaymentWithAllLinksBuilder withMoto(boolean moto) {
            this.moto = moto;
            return this;
        }

        public PaymentWithAllLinksBuilder withRefundSummary(RefundSummary refundSummary) {
            this.refundSummary = refundSummary;
            return this;
        }

        public PaymentWithAllLinksBuilder withSettlementSummary(PaymentSettlementSummary settlementSummary) {
            this.settlementSummary = settlementSummary;
            return this;
        }

        public PaymentWithAllLinksBuilder withCardDetails(CardDetails cardDetails) {
            this.cardDetails = cardDetails;
            return this;
        }

        public PaymentWithAllLinksBuilder withPaymentConnectorResponseLinks(List<PaymentConnectorResponseLink> paymentConnectorResponseLinks) {
            this.paymentConnectorResponseLinks = paymentConnectorResponseLinks;
            return this;
        }

        public PaymentWithAllLinksBuilder withSelfLink(URI selfLink) {
            this.selfLink = selfLink;
            return this;
        }

        public PaymentWithAllLinksBuilder withPaymentEventsUri(URI paymentEventsUri) {
            this.paymentEventsUri = paymentEventsUri;
            return this;
        }

        public PaymentWithAllLinksBuilder withPaymentCancelUri(URI paymentCancelUri) {
            this.paymentCancelUri = paymentCancelUri;
            return this;
        }

        public PaymentWithAllLinksBuilder withPaymentRefundsUri(URI paymentRefundsUri) {
            this.paymentRefundsUri = paymentRefundsUri;
            return this;
        }

        public PaymentWithAllLinksBuilder withPaymentCaptureUri(URI paymentCaptureUri) {
            this.paymentCaptureUri = paymentCaptureUri;
            return this;
        }

        public PaymentWithAllLinksBuilder withPaymentAuthorisationUri(URI paymentAuthorisationUri) {
            this.paymentAuthorisationUri = paymentAuthorisationUri;
            return this;
        }

        public PaymentWithAllLinksBuilder withCorporateCardSurcharge(Long corporateCardSurcharge) {
            this.corporateCardSurcharge = corporateCardSurcharge;
            return this;
        }

        public PaymentWithAllLinksBuilder withTotalAmount(Long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public PaymentWithAllLinksBuilder withProviderId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public PaymentWithAllLinksBuilder withMetadata(ExternalMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public PaymentWithAllLinksBuilder withFee(Long fee) {
            this.fee = fee;
            return this;
        }

        public PaymentWithAllLinksBuilder withNetAmount(Long netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public PaymentWithAllLinksBuilder withAuthorisationSummary(AuthorisationSummary authorisationSummary) {
            this.authorisationSummary = authorisationSummary;
            return this;
        }

        public PaymentWithAllLinksBuilder withAgreementId(String agreementId) {
            this.agreementId = agreementId;
            return this;
        }

        public PaymentWithAllLinksBuilder withAuthorisationMode(AuthorisationMode authorisationMode) {
            this.authorisationMode = authorisationMode;
            return this;
        }

        public PaymentWithAllLinks build() {
            return new PaymentWithAllLinks(chargeId, amount, state, returnUrl, description, reference, email, 
                    paymentProvider, createdDate, language, delayedCapture, moto, refundSummary, settlementSummary, 
                    cardDetails, paymentConnectorResponseLinks, selfLink, paymentEventsUri, paymentCancelUri, 
                    paymentRefundsUri, paymentCaptureUri, paymentAuthorisationUri, corporateCardSurcharge, totalAmount, 
                    providerId, metadata, fee, netAmount, authorisationSummary, agreementId, authorisationMode);
        }
    }
}
