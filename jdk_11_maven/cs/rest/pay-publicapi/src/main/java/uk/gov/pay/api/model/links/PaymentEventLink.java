package uk.gov.pay.api.model.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static javax.ws.rs.HttpMethod.GET;

@Schema(name = "PaymentEventLink", description = "Resource link for a payment of a payment event")
public class PaymentEventLink {

    public static final String PAYMENT_LINK = "payment_url";

    @JsonProperty(value = PAYMENT_LINK)
    private Link paymentLink;


    public PaymentEventLink(String href) {
        this.paymentLink = new Link(href, GET);
    }

    @Schema(name = PAYMENT_LINK)
    public Link getPaymentLink() {
        return paymentLink;
    }


    @Override
    public String toString() {
        return "PaymentEventLink{" +
                "paymentLink=" + paymentLink +
                '}';
    }


}
