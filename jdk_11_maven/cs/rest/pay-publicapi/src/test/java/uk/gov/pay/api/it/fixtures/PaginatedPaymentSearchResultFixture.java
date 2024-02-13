package uk.gov.pay.api.it.fixtures;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;

import java.util.List;

public class PaginatedPaymentSearchResultFixture {

    private int total;
    private int count;
    private int page;
    private List results;
    private PaymentNavigationLinksFixture _links;

    public static PaginatedPaymentSearchResultFixture aPaginatedPaymentSearchResult() {
        return new PaginatedPaymentSearchResultFixture();
    }

    public PaginatedPaymentSearchResultFixture withTotal(int total) {
        this.total = total;
        return this;
    }

    public PaginatedPaymentSearchResultFixture withCount(int count) {
        this.count = count;
        return this;
    }

    public PaginatedPaymentSearchResultFixture withPage(int page) {
        this.page = page;
        return this;
    }

    public PaginatedPaymentSearchResultFixture withLinks(PaymentNavigationLinksFixture links) {
        this._links = links;
        return this;
    }

    public PaginatedPaymentSearchResultFixture withPayments(List results) {
        this.results = results;
        return this;
    }

    public String build() {
        return new Gson()
                .toJson(this, new TypeReference<PaginatedPaymentSearchResultFixture>() {
        }.getType());
    }
}
