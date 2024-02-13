package uk.gov.pay.api.model.search.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.AuthorisationSummary;
import uk.gov.pay.api.model.CardDetails;
import uk.gov.pay.api.model.CardDetailsFromResponse;
import uk.gov.pay.api.model.CardPayment;
import uk.gov.pay.api.model.PaymentConnectorResponseLink;
import uk.gov.pay.api.model.PaymentSettlementSummary;
import uk.gov.pay.api.model.PaymentState;
import uk.gov.pay.api.model.RefundSummary;
import uk.gov.pay.api.model.TransactionResponse;
import uk.gov.pay.api.model.links.PaymentLinksForSearch;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.net.URI;
import java.util.List;

@Schema(name = "PaymentDetailForSearch")
public class PaymentForSearchResult extends CardPayment {

    @JsonProperty(LINKS_JSON_ATTRIBUTE)
    private PaymentLinksForSearch links = new PaymentLinksForSearch();

    public PaymentForSearchResult(String chargeId, long amount, PaymentState state, String returnUrl, String description,
                                  String reference, String email, String paymentProvider, String createdDate, SupportedLanguage language,
                                  boolean delayedCapture, boolean moto, RefundSummary refundSummary, PaymentSettlementSummary settlementSummary, CardDetails cardDetails,
                                  List<PaymentConnectorResponseLink> links, URI selfLink, URI paymentEventsLink, URI paymentCancelLink, URI paymentRefundsLink, URI paymentCaptureUri,
                                  Long corporateCardSurcharge, Long totalAmount, String providerId, ExternalMetadata externalMetadata,
                                  Long fee, Long netAmount, AuthorisationSummary authorisationSummary, AuthorisationMode authorisationMode) {
        
        super(chargeId, amount, state, returnUrl, description, reference, email, paymentProvider,
                createdDate, refundSummary, settlementSummary, cardDetails, language, delayedCapture, moto, corporateCardSurcharge, totalAmount, providerId, externalMetadata,
                fee, netAmount, authorisationSummary, null, authorisationMode);
        this.links.addSelf(selfLink.toString());
        this.links.addEvents(paymentEventsLink.toString());
        this.links.addRefunds(paymentRefundsLink.toString());

        if (!state.isFinished() && authorisationMode != AuthorisationMode.AGREEMENT) {
            this.links.addCancel(paymentCancelLink.toString());
        }
        if (links.stream().anyMatch(link -> "capture".equals(link.getRel()))) {
            this.links.addCapture(paymentCaptureUri.toString());
        }
    }

    public static PaymentForSearchResult valueOf(
            TransactionResponse paymentResult,
            URI selfLink,
            URI paymentEventsLink,
            URI paymentCancelLink,
            URI paymentRefundsLink,
            URI paymentCaptureUri) {
        
        return new PaymentForSearchResult(
                paymentResult.getTransactionId(),
                paymentResult.getAmount(),
                paymentResult.getState(),
                paymentResult.getReturnUrl(),
                paymentResult.getDescription(),
                paymentResult.getReference(),
                paymentResult.getEmail(),
                paymentResult.getPaymentProvider(),
                paymentResult.getCreatedDate(),
                paymentResult.getLanguage(),
                paymentResult.getDelayedCapture(),
                paymentResult.isMoto(),
                paymentResult.getRefundSummary(),
                paymentResult.getSettlementSummary(),
                paymentResult.getWalletType()
                        .map(wallet -> CardDetails.from(paymentResult.getCardDetailsFromResponse(), wallet.getTitleCase()))
                        .orElse(CardDetails.from(paymentResult.getCardDetailsFromResponse(), null)),
                paymentResult.getLinks(),
                selfLink,
                paymentEventsLink,
                paymentCancelLink,
                paymentRefundsLink,
                paymentCaptureUri,
                paymentResult.getCorporateCardSurcharge(),
                paymentResult.getTotalAmount(),
                paymentResult.getGatewayTransactionId(),
                paymentResult.getMetadata().orElse(null),
                paymentResult.getFee(),
                paymentResult.getNetAmount(),
                paymentResult.getAuthorisationSummary(),
                paymentResult.getAuthorisationMode());
    }

    public PaymentLinksForSearch getLinks() {
        return links;
    }
}
