package uk.gov.pay.api.model.links;

import io.swagger.v3.oas.annotations.media.Schema;

import static javax.ws.rs.HttpMethod.GET;

@Schema(name = "DisputeLinksForSearch", description = "links for search dispute resource")
public class DisputeLinksForSearch {

    private static final String PAYMENT = "payment";

    private Link payment;

    public Link getPayment() {
        return payment;
    }

    public void addPayment(String href) {
        this.payment = new Link(href, GET);
    }
}
