package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.pay.api.utils.CustomSupportedLanguageDeserializer;
import uk.gov.pay.api.utils.WalletDeserializer;
import uk.gov.service.payments.commons.api.json.ExternalMetadataDeserialiser;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResponse {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("payment_provider")
    private String paymentProvider;

    @JsonProperty("links")
    private List<PaymentConnectorResponseLink> links = new ArrayList<>();

    @JsonProperty(value = "refund_summary")
    private RefundSummary refundSummary;

    @JsonProperty(value = "settlement_summary")
    private PaymentSettlementSummary settlementSummary;

    @JsonProperty(value = "card_details")
    private CardDetailsFromResponse cardDetails;

    private Long amount;

    private PaymentState state;

    private String description;

    private String reference;

    private String email;

    @JsonDeserialize(using = CustomSupportedLanguageDeserializer.class)
    private SupportedLanguage language;

    @JsonProperty(value = "delayed_capture")
    private boolean delayedCapture;
    
    private boolean moto;

    @JsonProperty("corporate_card_surcharge")
    private Long corporateCardSurcharge;

    @JsonProperty("total_amount")
    private Long totalAmount;

    @JsonProperty("fee")
    private Long fee;

    @JsonProperty("net_amount")
    private Long netAmount;

    @JsonProperty(value = "created_date")
    private String createdDate;

    @JsonProperty(value = "gateway_transaction_id")
    private String gatewayTransactionId;

    @JsonDeserialize(using = ExternalMetadataDeserialiser.class)
    private ExternalMetadata metadata;

    @JsonProperty("authorisation_summary")
    private AuthorisationSummary authorisationSummary;
    
    @JsonProperty("authorisation_mode")
    private AuthorisationMode authorisationMode;

    @JsonProperty("wallet_type")
    @JsonDeserialize(using = WalletDeserializer.class)
    private Wallet walletType;

    public Optional<ExternalMetadata> getMetadata() {
        return Optional.ofNullable(metadata);
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

    /**
     * card brand is no longer a top level charge property. It is now at `card_details.card_brand` attribute
     * We still need to support `v1` clients with a top level card brand attribute to keep support their integrations.
     *
     * @return
     */
    @JsonProperty("card_brand")
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

    public CardDetailsFromResponse getCardDetailsFromResponse() {
        return cardDetails;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    }

    public AuthorisationMode getAuthorisationMode() {
        return authorisationMode;
    }

    public Optional<Wallet> getWalletType() {
        return Optional.ofNullable(walletType);
    }
}
