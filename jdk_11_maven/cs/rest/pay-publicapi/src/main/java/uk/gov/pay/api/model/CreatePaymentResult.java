package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.PaymentLinks;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static uk.gov.pay.api.model.CardPayment.LINKS_JSON_ATTRIBUTE;

/**
 * Defines swagger specs for create payment
 */
public class CreatePaymentResult {

    @JsonProperty
    @Schema(name = "amount", description = "The amount, in pence, the user has paid or will pay. " +
            "`amount` will match the value you sent in the request body.", example = "1200")
    private long amount;

    @JsonProperty
    private PaymentState state;

    @JsonProperty
    @Schema(description = "The description you sent in the request body when creating this payment.", example = "New passport application")
    private String description;

    @JsonProperty
    @Schema(description = "The reference number you associated with this payment.", example = "12345")
    private String reference;

    @JsonProperty
    @Schema(name = "language", description = "The language of the user’s payment page.", example = "en")
    private SupportedLanguage language;

    @JsonProperty
    @Schema(name = "payment_id", description = "The unique ID GOV.UK Pay automatically associated " +
            "with this payment when you created it.", example = "hu20sqlact5260q2nanm0q8u93")
    private String paymentId;

    @JsonProperty
    @Schema(name = "payment_provider", example = "worldpay")
    private String paymentProvider;

    @JsonProperty
    @Schema(name = "return_url", description = "The URL you direct the paying user to " +
            "after their payment journey on GOV.UK Pay ends.",
            example = "https://service-name.gov.uk/transactions/12345")
    private String returnUrl;

    @JsonProperty
    @Schema(name = "created_date", description = "The date you created the payment.", example = "2016-01-21T17:15:00.000Z")
    private String createdDate;

    @JsonProperty
    @Schema(name = "delayed_capture", description = "`delayed_capture` is `true` if you’re controlling " +
            "[when GOV.UK Pay takes (‘captures’) the payment from the paying user’s bank account]" +
            "(https://docs.payments.service.gov.uk/delayed_capture).", example = "false", accessMode = READ_ONLY)
    private boolean delayedCapture;

    @JsonProperty
    @Schema(description = "Indicates if this payment is a " +
            "[Mail Order / Telephone Order (MOTO) payment]" +
            "(https://docs.payments.service.gov.uk/moto_payments).", example = "false")
    private boolean moto;

    @JsonProperty("refund_summary")
    private RefundSummary refundSummary;

    @JsonProperty("settlement_summary")
    private PaymentSettlementSummary settlementSummary;

    @JsonProperty
    @Schema(name = LINKS_JSON_ATTRIBUTE, description = "API endpoints related to the payment.")
    private PaymentLinks links;

    @JsonProperty
    @Schema(name = "provider_id", description = "The reference number your " +
            "payment service provider associated with the payment.", example = "null")
    private String providerId;

    @JsonProperty
    @Schema(name = "metadata", description = "[Custom metadata](https://docs.payments.service.gov.uk/optional_features/custom_metadata/) you added to the payment.")
    private ExternalMetadata metadata;

    @JsonProperty
    @Schema(name = "email", description = "The paying user’s email address. " +
            "The paying user’s email field will be prefilled with this value when they make their payment. " +
            "`email` does not appear if you did not include it in the request body.", 
            example = "citizen@example.org")
    private String email;

    @JsonProperty(value = "card_details")
    @Schema(name = "card_details")
    private CardDetailsFromResponse cardDetails;
}
