package uk.gov.pay.api.model.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.PaymentConnectorResponseLink;
import uk.gov.pay.api.service.PublicApiUriGenerator;

import java.net.URI;
import java.util.List;

import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;

@Schema(name = "PaymentLinks", description = "links for payment")
public class PaymentLinks {
    
    private static final String SELF_FIELD = "self";
    private static final String NEXT_URL_FIELD = "next_url";
    private static final String NEXT_URL_POST_FIELD = "next_url_post";
    private static final String AUTH_URL_POST_FIELD = "auth_url_post";
    private static final String EVENTS_FIELD = "events";
    private static final String CANCEL_FIELD = "cancel";
    private static final String REFUNDS_FIELD = "refunds";
    private static final String CAPTURE_FIELD = "capture";

    @JsonProperty(value = SELF_FIELD)
    private Link self;

    @JsonProperty(NEXT_URL_FIELD)
    private Link nextUrl;

    @JsonProperty(NEXT_URL_POST_FIELD)
    private PostLink nextUrlPost;
    
    @JsonProperty(AUTH_URL_POST_FIELD)
    private PostLink authUrlPost;

    @JsonProperty(value = EVENTS_FIELD)
    private Link events;

    @JsonProperty(value = REFUNDS_FIELD)
    private Link refunds;

    @JsonProperty(value = CANCEL_FIELD)
    private PostLink cancel;
    
    @JsonProperty(value = CAPTURE_FIELD)
    private PostLink capture;

    public Link getSelf() {
        return self;
    }

    public Link getNextUrl() {
        return nextUrl;
    }

    public PostLink getNextUrlPost() {
        return nextUrlPost;
    }
    
    public PostLink getAuthUrlPost() {
        return authUrlPost;
    }

    public Link getEvents() {
        return events;
    }

    public Link getRefunds() {
        return refunds;
    }

    public PostLink getCancel() {
        return cancel;
    }

    public PostLink getCapture() {
        return capture;
    }

    public void addKnownLinksValueOf(List<PaymentConnectorResponseLink> chargeLinks, URI paymentAuthorisationUri) {
        addNextUrlIfPresent(chargeLinks);
        addNextUrlPostIfPresent(chargeLinks);
        addAuthUrlPostIfPresent(chargeLinks, paymentAuthorisationUri);
        addCaptureUrlIfPresent(chargeLinks);
    }

    public void addSelf(String href) {
        this.self = new Link(href, GET);
    }

    public void addEvents(String href) {
        this.events = new Link(href, GET);
    }

    public void addRefunds(String href) {
        this.refunds = new Link(href, GET);
    }

    public void addCancel(String href) {
        this.cancel = new PostLink(href, POST);
    }
    
    public void addCapture(String href) {
        this.capture = new PostLink(href, POST);
    }
    
    private void addAuthUrlPostIfPresent(List<PaymentConnectorResponseLink> chargeLinks, URI paymentAuthorisationUri) {
        chargeLinks.stream()
                .filter(links -> AUTH_URL_POST_FIELD.equals(links.getRel()))
                .findFirst()
                .ifPresent(links -> this.authUrlPost = new PostLink(paymentAuthorisationUri.toString(), links.getMethod(), links.getType(), links.getParams()));
    }

    private void addNextUrlPostIfPresent(List<PaymentConnectorResponseLink> chargeLinks) {
        chargeLinks.stream()
                .filter(chargeLink -> NEXT_URL_POST_FIELD.equals(chargeLink.getRel()))
                .findFirst()
                .ifPresent(chargeLink -> this.nextUrlPost = new PostLink(chargeLink.getHref(), chargeLink.getMethod(), chargeLink.getType(), chargeLink.getParams()));
    }

    private void addNextUrlIfPresent(List<PaymentConnectorResponseLink> chargeLinks) {
        chargeLinks.stream()
                .filter(chargeLink -> NEXT_URL_FIELD.equals(chargeLink.getRel()))
                .findFirst()
                .ifPresent(chargeLink -> this.nextUrl = new Link(chargeLink.getHref(), chargeLink.getMethod()));
    }

    private void addCaptureUrlIfPresent(List<PaymentConnectorResponseLink> chargeLinks) {
        chargeLinks.stream()
                .filter(chargeLink -> CAPTURE_FIELD.equals(chargeLink.getRel()))
                .findFirst()
                .ifPresent(chargeLink -> this.capture = new PostLink(chargeLink.getHref(), chargeLink.getMethod()));
    }
}
