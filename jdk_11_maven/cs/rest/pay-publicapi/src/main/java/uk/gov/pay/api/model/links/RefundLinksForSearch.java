package uk.gov.pay.api.model.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static javax.ws.rs.HttpMethod.GET;

@Schema(name = "RefundLinksForSearch", description = "links for search refunds resource")
public class RefundLinksForSearch {

    private static final String SELF = "self";
    private static final String PAYMENT = "payment";

    private Link self;
    private Link payment;

    @Schema(description = "self")
    @JsonProperty(value = SELF)
    public Link getSelf() {
        return self;
    }

    @Schema(description = "payment")
    @JsonProperty(value = PAYMENT)
    public Link getPayment() {
        return payment;
    }

    public void addSelf(String href) {
        this.self = new Link(href, GET);
    }

    public void addPayment(String href) {
        this.payment = new Link(href, GET);
    }
}
