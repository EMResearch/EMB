package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.api.model.telephone.PaymentOutcome;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChargeFromResponse {
    
    private String chargeId;
    
    private String returnUrl;
    
    private String paymentProvider;
    
    private List<PaymentConnectorResponseLink> links = new ArrayList<>();
    
    private RefundSummary refundSummary;

    private PaymentSettlementSummary settlementSummary;
    
    @JsonProperty("card_details")
    private CardDetailsFromResponse cardDetails;

    private Long amount;

    private PaymentState state;

    private String description;

    private String reference;

    private String email;
    
    private String telephoneNumber;

    @JsonProperty("agreement_id")
    private String agreementId;

    @JsonDeserialize(using = CustomSupportedLanguageDeserializer.class)
    private SupportedLanguage language;
    
    private boolean delayedCapture;
    
    private boolean moto;
    
    private Long corporateCardSurcharge;
    
    private Long totalAmount;
    
    private Long fee;
    
    private Long netAmount;
    
    private String createdDate;
    
    private String authorisedDate;
    
    private String processorId;
    
    private String providerId;
    
    private String authCode;
    
    private PaymentOutcome paymentOutcome;
    
    private String gatewayTransactionId;

    @JsonDeserialize(using = ExternalMetadataDeserialiser.class)
    private ExternalMetadata metadata;

    private AuthorisationSummary authorisationSummary;
    
    private AuthorisationMode authorisationMode;

    // wallet_type is a top level charge property but is returned as part of the card_details object
    @JsonProperty("wallet_type")
    @JsonDeserialize(using = WalletDeserializer.class)
    private Wallet walletType;

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

    public String getTelephoneNumber() {
        return telephoneNumber;
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

    public String getAgreementId() {
        return agreementId;
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

    public String getAuthorisedDate() {
        return authorisedDate;
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

    public PaymentOutcome getPaymentOutcome() {
        return paymentOutcome;
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

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    };

    public AuthorisationMode getAuthorisationMode() {
        return authorisationMode;
    }

    public Optional<Wallet> getWalletType() {
        return Optional.ofNullable(walletType);
    }
}
