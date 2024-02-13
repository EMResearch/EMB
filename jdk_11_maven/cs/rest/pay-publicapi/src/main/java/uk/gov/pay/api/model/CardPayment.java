package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.ExternalMetadataSerialiser;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.util.Optional;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@Schema(name = "CardPayment")
public class CardPayment {

    public static final String LINKS_JSON_ATTRIBUTE = "_links";

    @JsonProperty("payment_id")
    protected String paymentId;

    @JsonProperty("payment_provider")
    protected String paymentProvider;

    protected long amount;
    
    protected String description;
    
    protected String reference;
    
    @JsonProperty("created_date")
    protected String createdDate;
    @JsonProperty("refund_summary")
    private RefundSummary refundSummary;

    @JsonProperty("settlement_summary")
    private PaymentSettlementSummary settlementSummary;

    @JsonProperty("card_details")
    private CardDetails cardDetails;

    @JsonSerialize(using = ToStringSerializer.class)
    private SupportedLanguage language;

    @JsonProperty("delayed_capture")
    private boolean delayedCapture;
    
    @JsonProperty("moto")
    private boolean moto;

    @JsonProperty("corporate_card_surcharge")
    private Long corporateCardSurcharge;

    @JsonProperty("total_amount")
    private Long totalAmount;

    @JsonProperty("fee")
    private Long fee;

    @JsonProperty("net_amount")
    private Long netAmount;

    @JsonProperty("provider_id")
    @Schema(example = "reference-from-payment-gateway",
            description = "The unique ID your payment service provider generated for this payment. " +
                    "This is not the same as the `payment_id`.",
            accessMode = READ_ONLY)
    private String providerId;

    @JsonSerialize(using = ExternalMetadataSerialiser.class)
    @Schema(name = "metadata", example = "{\"property1\": \"value1\", \"property2\": \"value2\"}\"")
    private ExternalMetadata metadata;

    @JsonProperty("return_url")
    protected String returnUrl;
    
    protected String email;

    protected PaymentState state;

    //Used by Swagger to document the right model in the PaymentsResource
    @JsonIgnore
    protected String paymentType;

    @JsonProperty("authorisation_summary")
    private AuthorisationSummary authorisationSummary;
    
    @JsonProperty("agreement_id")
    private String agreementId;
    
    @JsonProperty("authorisation_mode")
    private AuthorisationMode authorisationMode;

    public CardPayment(String chargeId, long amount, PaymentState state, String returnUrl, String description,
                       String reference, String email, String paymentProvider, String createdDate,
                       RefundSummary refundSummary, PaymentSettlementSummary settlementSummary, CardDetails cardDetails,
                       SupportedLanguage language, boolean delayedCapture, boolean moto, Long corporateCardSurcharge, Long totalAmount,
                       String providerId, ExternalMetadata metadata, Long fee, Long netAmount, AuthorisationSummary authorisationSummary, String agreementId,
                       AuthorisationMode authorisationMode) {
        this.paymentId = chargeId;
        this.amount = amount;
        this.description = description;
        this.reference = reference;
        this.paymentProvider = paymentProvider;
        this.createdDate = createdDate;
        this.state = state;
        this.refundSummary = refundSummary;
        this.settlementSummary = settlementSummary;
        this.cardDetails = cardDetails;
        this.providerId = providerId;
        this.metadata = metadata;
        this.paymentType = TokenPaymentType.CARD.getFriendlyName();
        this.language = language;
        this.delayedCapture = delayedCapture;
        this.moto = moto;
        this.corporateCardSurcharge = corporateCardSurcharge;
        this.totalAmount = totalAmount;
        this.fee = fee;
        this.netAmount = netAmount;
        this.email = email;
        this.returnUrl = returnUrl;
        this.authorisationSummary = authorisationSummary;
        this.agreementId = agreementId;
        this.authorisationMode = authorisationMode;
    }

    /**
     * card brand is no longer a top level charge property. It is now at `card_details.card_brand` attribute
     * We still need to support `v1` clients with a top level card brand attribute to keep support their integrations.
     *
     * @return
     */
    @Schema(description = "This attribute is deprecated. Please use `card_details.card_brand` instead.",
            example = "Visa", accessMode = READ_ONLY, deprecated = true)
    @JsonProperty("card_brand")
    @Deprecated
    public String getCardBrand() {
        return cardDetails != null ? cardDetails.getCardBrand() : null;
    }

    public ExternalMetadata getMetadata() {
        return metadata;
    }

    public Optional<RefundSummary> getRefundSummary() {
        return Optional.ofNullable(refundSummary);
    }

    public Optional<PaymentSettlementSummary> getSettlementSummary() {
        return Optional.ofNullable(settlementSummary);
    }

    public Optional<CardDetails> getCardDetails() {
        return Optional.ofNullable(cardDetails);
    }

    @Schema(name = "language", example = "en", 
            description = "The ISO-6391 Alpha-2 code of the [language of the user's payment page]" + 
                    "(https://docs.payments.service.gov.uk/optional_features/welsh_language).")
    public SupportedLanguage getLanguage() {
        return language;
    }

    @Schema(description = "`delayed_capture` is `true` if you’re " +
            "[controlling how long it takes GOV.UK Pay to take (‘capture’) a payment]" +
            "(https://docs.payments.service.gov.uk/delayed_capture).", 
            example = "false", accessMode = READ_ONLY)
    public boolean getDelayedCapture() {
        return delayedCapture;
    }

    @Schema(description = "Indicates if this payment is a [Mail Order / Telephone Order (MOTO) payment]" +
            "(https://docs.payments.service.gov.uk/moto_payments).", example = "false")
    public boolean getMoto() { return moto; }

    @Schema(example = "250", description = "The [corporate card surcharge]" +
            "(https://docs.payments.service.gov.uk/corporate_card_surcharges/#add-corporate-card-fees) " +
            "amount in pence.",
            accessMode = READ_ONLY)
    public Optional<Long> getCorporateCardSurcharge() {
        return Optional.ofNullable(corporateCardSurcharge);
    }

    @Schema(example = "5", description = "The [payment service provider’s (PSP) transaction fee]" +
            "(https://docs.payments.service.gov.uk/reporting/#psp-fees), in pence. " +
            "`fee` only appears when we have taken (‘captured’) the payment from the user " +
            "or if their payment fails after they submitted their card details. " +
            "`fee` will not appear if your PSP is Worldpay or you are using an API key from a test service.",
            accessMode = READ_ONLY)
    public Optional<Long> getFee() {
        return Optional.ofNullable(fee);
    }

    @Schema(example = "1195", 
            description = "The amount, in pence, that will be paid into your bank account " +
                    "after your payment service provider takes the `fee`.",
            accessMode = READ_ONLY)
    public Optional<Long> getNetAmount() {
        return Optional.ofNullable(netAmount);
    }

    @Schema(example = "1450", 
            description = "Amount your user paid in pence, including corporate card fees. " +
            "`total_amount` only appears if you [added a corporate card surcharge to the payment]" + 
            "(https://docs.payments.service.gov.uk/corporate_card_surcharges/#add-corporate-card-fees).",
            accessMode = READ_ONLY)
    public Optional<Long> getTotalAmount() {
        return Optional.ofNullable(totalAmount);
    }

    public String getProviderId() {
        return providerId;
    }

    @Schema(example = "http://your.service.domain/your-reference", 
            description = "The URL you direct the paying user to after their payment journey on GOV.UK Pay ends.", 
            accessMode = READ_ONLY)
    public Optional<String> getReturnUrl() {
        return Optional.ofNullable(returnUrl);
    }

    @Schema(example = "The paying user’s email address.")
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public PaymentState getState() {
        return state;
    }

    @Schema(description = "Object containing information about the authentication of the payment.")
    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    }
    
    @Schema(hidden = true)
    public String getAgreementId() {
        return agreementId;
    }

    @Schema(type = "String", description = "How the payment will be authorised. Payments created in `web` mode require the paying user to visit the `next_url` to complete the payment.",
        allowableValues = {"web", "moto_api", "external"})
    public AuthorisationMode getAuthorisationMode() {
        return authorisationMode;
    }

    @Schema(example = "2016-01-21T17:15:00.000Z", accessMode = READ_ONLY)
    public String getCreatedDate() {
        return createdDate;
    }

    @Schema(example = "hu20sqlact5260q2nanm0q8u93",
            description = "The unique ID GOV.UK Pay automatically associated " +
                    "with this payment when you created it.",
            accessMode = READ_ONLY)
    public String getPaymentId() {
        return paymentId;
    }

    @Schema(example = "1200", description = "The description assigned to the payment when it was created.")
    public long getAmount() {
        return amount;
    }

    @Schema(example = "Your Service Description", description = "The description assigned to the payment when it was created.")
    public String getDescription() {
        return description;
    }

    @Schema(example = "your-reference",
            description = "The reference associated with the payment when it was created. " +
                    "`reference` is not unique - multiple payments can have the same `reference` value.")
    public String getReference() {
        return reference;
    }

    @Schema(example = "worldpay",
            description = "The payment service provider that processed this payment.",
            accessMode = READ_ONLY)
    public String getPaymentProvider() {
        return paymentProvider;
    }

    @Override
    public String toString() {
        // Don't include:
        // description - some services include PII
        // reference - can come from user input for payment links, in the past they have mistakenly entered card numbers
        // return url - services can include identifiers that are incorrectly flagged as PII or card numbers
        return "Card Payment{" +
                "paymentId='" + paymentId + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", cardBrandLabel='" + getCardBrand() + '\'' +
                ", amount=" + amount +
                ", fee=" + fee +
                ", netAmount=" + netAmount +
                ", corporateCardSurcharge='" + corporateCardSurcharge + '\'' +
                ", state='" + state + '\'' +
                ", language='" + language.toString() + '\'' +
                ", delayedCapture=" + delayedCapture +
                ", moto=" + moto +
                ", createdDate='" + createdDate + '\'' +
                ", agreementId='" + agreementId + '\'' +
                '}';
    }
}
