package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.PaymentEventLink;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(name = "PaymentEvent", description = "A List of Payment Events information")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEventResponse {
    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("state")
    private PaymentState state;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("_links")
    private PaymentEventLink paymentLink;

    public static PaymentEventResponse from(PaymentEvent paymentEvent, String paymentId, String paymentLink) {
        return new PaymentEventResponse(paymentId, paymentEvent.getState(), paymentEvent.getUpdated(), paymentLink);
    }

    public static PaymentEventResponse from(TransactionEvent event, String paymentId, String paymentLink) {
        return new PaymentEventResponse(paymentId, event.getState(), event.getTimestamp(), paymentLink);
    }

    private PaymentEventResponse(String paymentId, PaymentState state, String updated, String paymentLink) {
        this.paymentId = paymentId;
        this.state = state;
        this.updated = updated;
        this.paymentLink = new PaymentEventLink(paymentLink);
    }

    @Schema(example = "hu20sqlact5260q2nanm0q8u93", 
            description = "The unique ID GOV.UK Pay automatically associated with this payment when you created it.", 
            accessMode = READ_ONLY)
    public String getPaymentId() {
        return paymentId;
    }

    @Schema(description = "state")
    public PaymentState getState() {
        return state;
    }

    @Schema(description = "When this payment’s state changed. " +
            "This value uses Coordinated Universal Time (UTC) and ISO-8601 format - `YYYY-MM-DDThh:mm:ss.SSSZ`.", 
            example = "2017-01-10T16:44:48.646Z", accessMode = READ_ONLY)
    public String getUpdated() {
        return updated;
    }

    public PaymentEventLink getPaymentLink() {
        return paymentLink;
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", state='" + state + '\'' +
                ", updated=" + updated +
                ", paymentLink=" + paymentLink +
                '}';
    }
}
