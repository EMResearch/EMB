package uk.gov.pay.api.it.fixtures;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;

import java.util.List;

public class PaginatedTransactionSearchResultFixture {

    private int total;
    private int count;
    private int page;
    private List results;
    private PaymentNavigationLinksFixture _links;

    public static PaginatedTransactionSearchResultFixture aPaginatedTransactionSearchResult() {
        return new PaginatedTransactionSearchResultFixture();
    }

    public PaginatedTransactionSearchResultFixture withTotal(int total) {
        this.total = total;
        return this;
    }

    public PaginatedTransactionSearchResultFixture withCount(int count) {
        this.count = count;
        return this;
    }

    public PaginatedTransactionSearchResultFixture withPage(int page) {
        this.page = page;
        return this;
    }

    public PaginatedTransactionSearchResultFixture withLinks(PaymentNavigationLinksFixture links) {
        this._links = links;
        return this;
    }

    public PaginatedTransactionSearchResultFixture withPayments(List results) {
        this.results = results;
        return this;
    }

    public String build() {
        return new Gson()
                .toJson(this, new TypeReference<PaginatedTransactionSearchResultFixture>() {
        }.getType());
    }
}
