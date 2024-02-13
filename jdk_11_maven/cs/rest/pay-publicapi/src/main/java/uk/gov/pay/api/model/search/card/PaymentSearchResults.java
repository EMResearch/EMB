package uk.gov.pay.api.model.search.card;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.SearchNavigationLinks;
import uk.gov.pay.api.model.search.SearchPagination;

import java.util.List;

/**
 * Used to define swagger specs for search results.
 */
public class PaymentSearchResults implements SearchPagination {

    @Schema(name = "total", example = "100", description = "Total number of payments matching your search criteria.")
    private int total;
    @Schema(name = "count", example = "20", description = "Number of payments on the current page of search results.")
    private int count;
    @Schema(name = "page", example = "1", 
            description = "The [page of results you’re viewing]" +
                    "(https://docs.payments.service.gov.uk/api_reference/#pagination). " +
                    "To view other pages, make this request again using the `page` parameter.")
    private int page;
    private List<PaymentForSearchResult> results;
    @Schema(name = "_links")
    SearchNavigationLinks links;

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

    @Schema(name = "results", description = "Contains payments matching your search criteria.")
    public List<PaymentForSearchResult> getPayments() {
        return results;
    }

    @Override
    public SearchNavigationLinks getLinks() {
        return links;
    }
}
