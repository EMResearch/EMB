package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

@Schema(name = "PaymentRefundRequest", description = "The Payment Refund Request Payload")
public class CreatePaymentRefundRequest {

    public static final String REFUND_AMOUNT_AVAILABLE = "refund_amount_available";
    public static final int REFUND_MIN_VALUE = 1;

    @Schema(description = "The amount you want to [refund to your user]" +
            "(https://docs.payments.service.gov.uk/refunding_payments/) in pence.", required = true,
            example = "150000", minimum = "1", maximum = "10000000")
    private int amount;
    @JsonProperty("refund_amount_available")
    @Schema(description = "Amount in pence. Total amount still available before issuing the refund", required = false,
            example = "200000", minimum = "1", maximum = "10000000")
    private Integer refundAmountAvailable;

    public CreatePaymentRefundRequest() {
    }

    public CreatePaymentRefundRequest(int amount, Integer refundAmountAvailable) {
        this.amount = amount;
        this.refundAmountAvailable = refundAmountAvailable;
    }

    public int getAmount() {
        return amount;
    }

    /**
     * This field should be made compulsory at a later stage
     *
     * @return
     */
    public Optional<Integer> getRefundAmountAvailable() {
        return Optional.ofNullable(refundAmountAvailable);
    }

    @Override
    public String toString() {
        return "CreatePaymentRefundRequest{" +
                "amount=" + amount +
                ", refundAmountAvailable=" + refundAmountAvailable +
                '}';
    }
}
