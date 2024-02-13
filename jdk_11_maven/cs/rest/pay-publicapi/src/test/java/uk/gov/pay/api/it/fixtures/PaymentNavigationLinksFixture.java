package uk.gov.pay.api.it.fixtures;

import uk.gov.pay.api.model.links.Link;

public class PaymentNavigationLinksFixture {

    private Link self;
    private Link first_page;
    private Link last_page;
    private Link prev_page;
    private Link next_page;

    public PaymentNavigationLinksFixture withSelfLink(String href) {
        this.self = new Link(href);
        return this;
    }
    public PaymentNavigationLinksFixture withPrevLink(String href) {
        this.prev_page = new Link(href);
        return this;
    }
    public PaymentNavigationLinksFixture withNextLink(String href) {
        this.next_page = new Link(href);
        return this;
    }
    public PaymentNavigationLinksFixture withFirstLink(String href) {
        this.first_page = new Link(href);
        return this;
    }
    public PaymentNavigationLinksFixture withLastLink(String href) {
        this.last_page = new Link(href);
        return this;
    }
}
