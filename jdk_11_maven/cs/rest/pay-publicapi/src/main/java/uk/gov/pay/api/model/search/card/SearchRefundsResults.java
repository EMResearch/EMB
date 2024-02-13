package uk.gov.pay.api.model.search.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.SearchNavigationLinks;
import uk.gov.pay.api.model.search.SearchPagination;

import java.util.List;

@Schema(name = "RefundSearchResults")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchRefundsResults implements SearchPagination {

    @Schema(example = "100", description = "Number of refunds matching your search criteria.")
    private int total;
    @Schema(example = "20", description = "Number of refunds on the current page of search results.")
    private int count;
    @Schema(example = "1", description = "The [page of results](payments.service.gov.uk/api_reference/#pagination) you’re viewing. To view other pages, make this request again using the `page` parameter.")
    private int page;
    
    @Schema(description = "Contains the refunds matching your search criteria.")
    private List<RefundForSearchRefundsResult> results;
    @JsonProperty("_links")
    private SearchNavigationLinks links;

    public SearchRefundsResults(int total, int count, int page, List<RefundForSearchRefundsResult> results,
                                    SearchNavigationLinks links) {
        this.total = total;
        this.count = count;
        this.page = page;
        this.results = results;
        this.links = links;
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

    public List<RefundForSearchRefundsResult> getResults() {
        return results;
    }

    @Override
    public SearchNavigationLinks getLinks() {
        return links;
    }
}
