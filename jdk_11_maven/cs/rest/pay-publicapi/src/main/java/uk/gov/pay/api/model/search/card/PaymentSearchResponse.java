package uk.gov.pay.api.model.search.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.api.model.links.SearchNavigationLinks;
import uk.gov.pay.api.model.search.SearchPagination;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSearchResponse<T> implements SearchPagination {

    @JsonProperty("total")
    private int total;

    @JsonProperty("count")
    private int count;

    @JsonProperty("page")
    private int page;

    @JsonProperty("results")
    private List<T> payments;

    @JsonProperty("_links")
    private SearchNavigationLinks links = new SearchNavigationLinks();

    public List<T> getPayments() {
        return payments;
    }
    @Override
    public int getTotal() {
        return total;
    }
    @Override
    public int getCount() {
        return count;
    }
    @Override
    public int getPage() {
        return page;
    }

    public SearchNavigationLinks getLinks() {
        return links;
    }
}
