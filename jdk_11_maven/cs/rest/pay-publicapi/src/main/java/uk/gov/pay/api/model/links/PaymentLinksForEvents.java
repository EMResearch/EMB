package uk.gov.pay.api.model.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static javax.ws.rs.HttpMethod.GET;

@Schema(name = "PaymentLinksForEvents", description = "links for events resource")
public class PaymentLinksForEvents {

    public static final String SELF = "self";

    @JsonProperty(value = SELF)
    private Link self;

    @Schema(description = SELF)
    public Link getSelf() {
        return self;
    }

    public void addSelf(String href) {
        this.self = new Link(href, GET);
    }

}
