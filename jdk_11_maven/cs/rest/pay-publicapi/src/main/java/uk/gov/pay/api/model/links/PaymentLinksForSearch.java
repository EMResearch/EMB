package uk.gov.pay.api.model.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;

@Schema(name = "PaymentLinksForSearch", description = "links for search payment resource")
public class PaymentLinksForSearch {

    private static final String SELF = "self";
    private static final String EVENTS = "events";
    private static final String CANCEL = "cancel";
    private static final String REFUNDS = "refunds";
    private static final String CAPTURE = "capture";

    @JsonProperty(value = SELF)
    private Link self;

    @JsonProperty(value = CANCEL)
    private PostLink cancel;

    @JsonProperty(value = EVENTS)
    private Link events;

    @JsonProperty(value = REFUNDS)
    private Link refunds;
    
    @JsonProperty(value = CAPTURE)
    private PostLink capture;

    public Link getSelf() {
        return self;
    }

    public PostLink getCancel() {
        return cancel;
    }

    public Link getEvents() {
        return events;
    }

    public Link getRefunds() {
        return refunds;
    }

    @Schema(name = CAPTURE, implementation = PostLink.class)
    public Link getCapture() {
        return capture;
    }

    public void addSelf(String href) {
        this.self = new Link(href, GET);
    }

    public void addEvents(String href) {
        this.events = new Link(href, GET);
    }

    public void addCancel(String href) {
        this.cancel = new PostLink(href, POST);
    }

    public void addRefunds(String href) {
        this.refunds = new Link(href, GET);
    }

    public void addCapture(String href) {
        this.capture = new PostLink(href, POST);
    }
}
